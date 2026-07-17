# Codeforces 수집 배치 힙 사용량 측정 보고서

> String 통짜 파싱 → InputStream 스트리밍 파싱 개선 전후 비교

| | |
|---|---|
| **측정일** | 2026-07-17 |
| **대상** | `codeforcesIngestJob` (허깅페이스 `open-r1/codeforces` 데이터셋 수집 배치) |
| **비교 브랜치** | `perf-before` (String 통짜 파싱) / `perf-after` (InputStream 스트리밍 파싱) |
| **결론** | 동일 입력(3,000건) 기준 피크 힙 **1,570M → 105M (약 15배 축소)**. 512M 힙에서 before는 6번째 페이지 파싱 중 OOM, after는 128M 힙으로도 완주 (Full GC 0회) |

---

## 1. 배경

운영 환경(t3.micro, RAM 1GB → JVM 힙 약 250MB)에서 코드포스 수집 배치 실행 중 `OutOfMemoryError`가 발생했다. 원인은 허깅페이스 API 응답 처리 방식으로 추정되었다:

- 응답 전체를 `String`으로 수신한 뒤 `ObjectMapper.readTree()`로 통짜 JsonNode 트리를 구성
- 이 시점의 힙에는 ① 응답 String 전체, ② 전체 JSON 트리, ③ 하류(청크)로 전달된 100건의 full-fat JsonNode가 동시에 존재
- 데이터셋 행에는 파서가 사용하지 않는 대용량 필드(`generated_tests`, 풀이 코드 등)가 포함되어 있어, 한 페이지(100건) 응답만으로도 수백 MB급 힙이 필요

개선판은 응답 body를 `InputStream`으로 받아 Jackson `JsonParser`로 토큰 단위 순회하며, 파서가 실제로 사용하는 필드(KEEP 10개)만 슬림 노드로 추출하고 나머지는 `skipChildren()`으로 읽는 즉시 버린다. 당시 수정으로 운영 OOM은 해소되었으나 **정량 측정 없이 배포**되어, 본 측정으로 개선 효과를 수치화했다.

## 2. 측정 방법

### 2.1 비교 구도 — 단일 변수 통제

두 브랜치는 파싱 방식만 다르고 잡 구조(Reader → Processor → Writer), 청크 크기, DB 적재 로직이 동일하다. 따라서 힙 사용량 차이는 파싱 방식 차이로 귀속된다.

| | before (`perf-before`) | after (`perf-after`) |
|---|---|---|
| 응답 수신 | `body(String.class)` — 전체를 힙에 적재 | `exchange()` — `InputStream` 스트림 |
| 파싱 | `readTree()` 통짜 트리 | `JsonParser` 토큰 단위 순회 |
| 하류 전달 | full-fat JsonNode (모든 필드 포함) | KEEP 필드만 담은 슬림 ObjectNode |
| 불필요 필드 | 트리에 그대로 상주 | `skipChildren()`으로 즉시 폐기 |

### 2.2 측정 하네스 — 배치 동기 실행 테스트

운영 경로(관리자 API 트리거)는 비동기라 실행 JVM과 종료 시점을 특정하기 어렵다. 대신 `BatchMeasurementTest`에서 `JobOperator.start()`를 **동기 호출**하여, surefire가 fork한 테스트 JVM 안에서 배치 1회가 처음부터 끝까지 돌게 했다.

```java
@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
class BatchMeasurementTest {
    @Autowired JobOperator jobOperator;
    @Autowired Job codeforcesIngestJob;

    @Test
    void 배치_동기_실행_힙측정() throws Exception {
        JobParameters params = new JobParametersBuilder()
                .addLong("measureTime", System.currentTimeMillis())
                .toJobParameters();
        JobExecution execution = jobOperator.start(codeforcesIngestJob, params);
        // 종료 status 와 소요시간 출력
    }
}
```

- fork된 테스트 JVM에 `-Xmx`와 GC 로깅을 걸면 **배치 1회 실행 = JVM 1개 = GC 로그 1개**로 깔끔하게 대응된다.
- `measureTime` 파라미터로 매 실행이 새 JobInstance가 되게 하여, 이전 실행의 재시작(offset 복구)이 끼어들지 않게 했다.

### 2.3 실행 시간 통제 — 페이지 상한 옵션

데이터셋 전량(10,000건)을 돌리면 회당 수십 분이라 반복 측정이 불가능하다. Reader에 `codeforces.batch.max-pages` 옵션(기본 0 = 무제한, 운영 동작 불변)을 추가하고 30페이지로 상한을 걸었다.

```java
// nextAvailablePage() 안, 페이지 요청 직전
if (maxPages > 0 && offset >= (long) maxPages * pageSize) {
    return null; // 데이터셋 끝과 동일한 정상 종료 경로
}
```

- `null` 반환은 기존 "데이터셋 끝" 신호와 같은 경로라 스텝이 정상 COMPLETED로 끝난다 — 인위적 중단이 아니라 "입력이 30페이지짜리"인 것과 동일하게 동작.
- 이 커밋을 `perf-before`에 만들고 `perf-after`에 cherry-pick하여 양쪽 상한 코드가 동일하다.
- 30페이지 × 100건 = **3,000건**, 실측 회당 약 3분.

### 2.4 공정성 통제 — 실행 조건 시스템 프로퍼티 통일

두 브랜치의 `application.properties`가 서로 달랐다(before: page-size 10·딜레이 300ms / after: page-size 100·딜레이 1000ms). 파일값 그대로면 불공정 비교이므로, 파일은 건드리지 않고 **fork JVM에 시스템 프로퍼티로 주입**해 통일했다(Spring 프로퍼티 우선순위상 시스템 프로퍼티가 파일값을 이긴다).

```bash
# backend/ 에서 실행 — 브랜치 전환 시 clean 필수
./mvnw clean test -Dtest=BatchMeasurementTest -DargLine="\
  -Xmx512m \
  -Xlog:gc*:file=gc-before-512m.log \
  -Dcodeforces.batch.page-size=100 \
  -Dcodeforces.batch.max-pages=30 \
  -Dcodeforces.batch.request-delay-ms=300"
```

통일된 조건: **page-size 100**(허깅페이스 API 상한, 초과 시 422), **max-pages 30**, **요청 딜레이 300ms**. 이로써 두 실행의 유일한 차이는 소스 코드가 된다.

### 2.5 반복 실행 안전장치

- Writer는 `INSERT IGNORE`라 같은 3,000건을 여러 번 넣어도 멱등 — 실행 간 DB 상태 차이가 측정에 영향 없음
- Processor(`CodeforcesParser`)는 순수 파싱으로 외부 호출이 없어, 실행 간 변동 요인은 허깅페이스 응답 시간뿐
- `spring.batch.job.enabled=false`라 컨텍스트 기동 시 잡이 자동 실행되지 않음 — 실행은 테스트의 `start()` 1회뿐
- 브랜치 전환 시마다 `mvnw clean` 수행 (생략 시 이전 브랜치 클래스가 섞여 `IncompatibleClassChangeError` 발생)

### 2.6 데이터 수집과 판정 기준

**피크 힙**: `-Xlog:gc*` 통합 GC 로그의 모든 이벤트에서 `사용량(GC직전)M->사용량(GC직후)M(커밋힙M)` 패턴을 추출하여,

- 피크 힙 = max(GC 직전 사용량) — "실제로 이만큼 필요했다"
- 피크 잔존 = max(GC 직후 사용량) — GC로도 회수 못 하는 라이브 데이터 크기
- Full GC 횟수 = `Pause Full` 이벤트 수

**성공/실패 판정**은 GC 로그와 독립적으로 배치 자체 출력으로 교차 확인:

- 테스트 출력의 `배치 종료 status=COMPLETED/FAILED`
- FAILED 시 스택트레이스의 `OutOfMemoryError` 발생 지점 (`ChunkOrientedStep.readChunk` → read 단계)
- Reader 로그 `[CF-READ] offset=N 에서 100건 확보`의 마지막 offset으로 도달 페이지 특정

> 참고: G1 GC는 `-Xmx`를 전부 커밋하지 않고 필요한 만큼만 힙을 확장한다. 커밋 힙 크기 자체도 "얼마나 필요했는가"의 부가 증거로 활용했다.

## 3. 측정 결과

### 3.1 실행 매트릭스 (총 4회)

| # | 브랜치 | 힙 | 결과 | 소요 | 피크 힙(GC 직전) | 피크 잔존(GC 직후) | Full GC |
|---|---|---|---|---|---|---|---|
| 1 | before | `-Xmx512m` | **FAILED (OOM)** — 6번째 페이지(offset=500) 파싱 중 | 53초 | 507M (한계 도달) | 371M | **12회** |
| 2 | after | `-Xmx512m` | COMPLETED (3,000건) | 190초 | 124M | 79M | 0회 |
| 3 | after | `-Xmx128m` | COMPLETED (3,000건) | 189초 | **105M** | 68M | 0회 |
| 4 | before | `-Xmx2g` | COMPLETED (3,000건) | 195초 | **1,570M** (커밋 1,627M) | 677M | 0회 |

### 3.2 해석

**before — 한 페이지 파싱에 GB급 힙이 필요하다.**
2G 힙에서 완주했을 때 피크가 1,570M까지 치솟았다. 응답 String + 통짜 트리 + full-fat 청크가 동시에 힙에 얹히는 구조 탓이며, GC 직후에도 677M이 살아남는 것은 청크에 물린 full-fat JsonNode들이 회수 불가능한 라이브 데이터로 상주함을 보여준다. 512M 힙에서는 5페이지(500건)까지 처리한 뒤 6번째 페이지에서 Full GC를 12회 반복하다 read 단계에서 OOM으로 사망했다. 운영 t3.micro(힙 약 250MB)에서 관찰된 장애와 정확히 일치하는 재현이다.

**after — 페이지 크기와 무관하게 힙이 평탄하다.**
동일 입력에서 피크 105~124M, Full GC 0회로 완주했다. 512M을 허용한 실행(#2)에서도 G1이 힙을 134M까지만 커밋했다 — 그 이상이 필요한 순간 자체가 없었다는 뜻이다. 무거운 필드를 스트림에서 즉시 폐기하므로 힙에 쌓이는 것은 슬림 노드 100건뿐이며, 이는 t3.micro급 협소 힙(#3, 128M)에서도 여유 있게 완주하는 수준이다.

### 3.3 결론

> **재현 테스트 기준, 동일 입력(30페이지 × 100건 = 3,000건)에서 피크 힙 1,570M → 105M (약 15배 축소). 512M 힙에서 String 통짜 파싱은 6번째 페이지 파싱 중 OOM으로 실패했고, 스트리밍 파싱은 그 1/4인 128M 힙으로도 Full GC 없이 완주했다.**

## 4. 후속 제안

- 운영 워크어라운드 정리: OOM 회피용으로 낮췄던 `page-size` 축소(100→20, 커밋 `48c577f`)는 스트리밍 파싱 기준 불필요하다. 측정상 page-size 100이 t3.micro 힙에서도 안전하므로 되돌리면 수집 소요 시간과 API 호출 횟수를 줄일 수 있다.
- `codeforces.batch.max-pages` 옵션은 측정용으로 추가되었으나 기본값 0(무제한)으로 운영 동작에 영향이 없고, 부분 수집·스모크 테스트 용도로도 활용 가능하다.

## 부록 A. 재현 절차

```bash
# 0. 사전 조건: 로컬 MySQL(algolist, batch_system 스키마) 기동
#    BatchMeasurementTest 는 untracked 파일이라 브랜치 전환에도 워킹트리에 유지됨

# 1. before @512m — OOM 재현
git checkout perf-before
cd backend
./mvnw clean test -Dtest=BatchMeasurementTest -DargLine="-Xmx512m -Xlog:gc*:file=gc-before-512m.log -Dcodeforces.batch.page-size=100 -Dcodeforces.batch.max-pages=30 -Dcodeforces.batch.request-delay-ms=300"

# 2. after @512m / @128m — 완주 + 피크 측정
git checkout perf-after
./mvnw clean test -Dtest=BatchMeasurementTest -DargLine="-Xmx512m -Xlog:gc*:file=gc-after-512m.log -Dcodeforces.batch.page-size=100 -Dcodeforces.batch.max-pages=30 -Dcodeforces.batch.request-delay-ms=300"
./mvnw test -Dtest=BatchMeasurementTest -DargLine="-Xmx128m -Xlog:gc*:file=gc-after-128m.log -Dcodeforces.batch.page-size=100 -Dcodeforces.batch.max-pages=30 -Dcodeforces.batch.request-delay-ms=300"

# 3. before @2g — before 의 실제 피크 측정
git checkout perf-before
./mvnw clean test -Dtest=BatchMeasurementTest -DargLine="-Xmx2g -Xlog:gc*:file=gc-before-2g.log -Dcodeforces.batch.page-size=100 -Dcodeforces.batch.max-pages=30 -Dcodeforces.batch.request-delay-ms=300"
```

## 부록 B. GC 로그 피크 추출 스크립트

```bash
#!/bin/sh
# 사용법: peak_heap.sh <gc.log>
grep -oE '[0-9]+M->[0-9]+M\([0-9]+M\)' "$1" \
  | sed -E 's/^([0-9]+)M->([0-9]+)M\(([0-9]+)M\)/\1 \2 \3/' \
  | awk '{ if ($1>b) b=$1; if ($2>a) a=$2; if ($3>t) t=$3; n++ }
         END { printf "GC 이벤트 %d회 / 피크 힙(GC직전) %dM / 피크 잔존(GC직후) %dM / 최대 커밋 힙 %dM\n", n, b, a, t }'
```

## 부록 C. 측정 환경

| 항목 | 값 |
|---|---|
| OS | Windows 11 |
| 실행 방식 | Maven surefire fork JVM (`./mvnw test -Dtest=BatchMeasurementTest`) |
| GC | G1 (JVM 기본) |
| GC 로깅 | `-Xlog:gc*:file=<파일>` |
| DB | 로컬 MySQL (`algolist` + `batch_system`) |
| 데이터 소스 | `datasets-server.huggingface.co` `open-r1/codeforces` (train split, 전체 약 10,000건 중 앞 3,000건) |
| Spring Batch | 6.0.3 (Spring Boot 4.x) |
| 상한 커밋 | `perf-before` `5443729` / `perf-after` `da52055` |
