# 문제 검색 DB 커넥션 점유 시간 측정 보고서

> 검색 플로우 전체 `@Transactional` → DB 작업만 ProblemWriter 로 분리, 개선 전후 비교

| | |
|---|---|
| **측정일** | 2026-07-17 |
| **대상** | `ProblemServiceImpl.searchProblem()` — DB 미스 시 GitHub 검색 → 파싱 → 이미지 처리 → 저장 → 재조회 |
| **비교** | before: 메서드 전체 `@Transactional` (브랜치 `conn-before`) / after: DB 작업만 `ProblemWriter.save()` 트랜잭션 (main) |
| **결론** | 요청당 커넥션 점유 **3,230ms → 31ms**. before 는 GitHub 외부 I/O 내내 커넥션 1개를 점유(요청 시간의 99.7%), after 는 실제 DB 작업 순간에만 짧게 점유(0.6%) |

## 1. 배경

문제 검색은 DB에 없으면 GitHub 코드 검색 → README fetch → 파싱 → 이미지 다운로드(base64 변환) → DB 저장 → 재조회로 이어지는, **외부 I/O가 수 초를 차지하는** 플로우다. 과거에는 이 메서드 전체가 `@Transactional` 로 묶여 있어 트랜잭션 시작 시점에 꺼낸 DB 커넥션이 GitHub I/O 가 끝날 때까지 반납되지 않았고, 커넥션 풀 점유가 컸다. DB 저장만 별도 클래스 `ProblemWriter.save()` 의 트랜잭션으로 분리해 해결했으며, 본 측정으로 그 효과를 수치화했다.

## 2. 측정 방법

- **계측**: 테스트 설정에서 비즈니스 데이터소스(`algoListDataSource`, HikariCP)를 데코레이터로 감싸 `getConnection()`(체크아웃) ~ `close()`(반납) 구간을 건별로 기록. 트랜잭션이 걸려 있으면 커밋까지 반납되지 않으므로 "커넥션을 몇 번, 각각 얼마나 물고 있었는지"가 그대로 드러난다.
- **before 재현**: 현재 main 에서 `searchProblem` 에 `@Transactional` 하나만 추가한 브랜치 `conn-before` 를 만들어 단일 변수 비교. (`ProblemWriter.save()` 의 `@Transactional` 은 REQUIRED 라 바깥 트랜잭션에 합류 → 과거 구조와 동일)
- **실행**: `ConnectionHoldMeasurementTest` 에서 `searchProblem("피보나치")` 호출. 한글 검색어라 DB(영문 codeforces 타이틀)와 미스 → GitHub 경로 강제. 테스트가 실행 전후로 매칭 행을 삭제해 두 실행 모두 동일하게 "DB 미스"에서 시작.
- **공정성 확인**: 두 실행이 GitHub 검색 결과로 같은 README 5개를 같은 순서로 처리했음을 로그로 확인. GitHub 토큰은 `application-secret.properties` 로 주입.

```bash
# backend/ 에서, 브랜치 전환 시 clean 필수
./mvnw clean test -Dtest=ConnectionHoldMeasurementTest
```

## 3. 결과

| | before (`@Transactional` 통짜) | after (ProblemWriter 분리) |
|---|---|---|
| 검색 전체 소요(wall) | 3,240ms | 5,133ms |
| 커넥션 체크아웃 | **1회** | 4회 |
| 커넥션 점유 합계 | **3,230ms** (요청의 99.7%) | **31ms** (요청의 0.6%) |
| 최장 단일 점유 | 3,230ms | 18ms |

**after 체크아웃 타임라인** — GitHub 구간(약 5초) 동안 점유 커넥션 0개:

```
#1 최초 DB 검색   : 점유 5ms   (+8ms 시점)
   (GitHub 검색 + README 5건 fetch/파싱/이미지 ≈ 5.1초 — 커넥션 미점유)
#2 save 트랜잭션  : 점유 18ms  (+5,108ms 시점)
#3, #4 재조회     : 점유 3ms, 5ms
```

**해석**

- before 는 체크아웃 1회가 요청 전체를 덮는다. 트랜잭션 매니저가 메서드 진입 시 커넥션을 꺼내 커밋까지 반납하지 않으므로, GitHub 응답을 기다리는 동안에도 풀의 커넥션 하나가 잠긴다. 기본 풀(10개) 기준 동시 검색 10건이면 풀 전체가 고갈되어 일반 웹 요청까지 대기/타임아웃된다.
- after 는 DB 접점 순간에만 짧게 빌렸다 반납한다. 체크아웃 횟수가 늘어난 것은 낭비가 아니다 — 풀에서 빌리는 비용은 마이크로초 단위이며, 외부 I/O 5초 동안 커넥션을 풀에 돌려놓았다는 증거다.
- wall time 차이(3.2초 vs 5.1초)는 GitHub API 응답 편차다(동일 쿼리 반복으로 뒤에 실행된 before 가 GitHub 캐시 이득). 이 측정의 비교 지표는 wall 이 아니라 점유 시간이며, **before 는 wall 이 더 짧았는데도 점유는 100배 이상 길었다**는 점이 오히려 논지를 강화한다.

### 결론

> **재현 테스트 기준, 동일 검색 요청에서 DB 커넥션 점유가 요청당 3,230ms → 31ms (약 1/100). 트랜잭션 경계를 외부 I/O 와 분리한 것만으로 커넥션 풀 고갈 리스크가 제거되었다.**

## 부록. 관련 위치

| 항목 | 위치 |
|---|---|
| 측정 테스트 | `backend/src/test/java/com/algolist/backend/ConnectionHoldMeasurementTest.java` (untracked, `.git/info/exclude` 등록) |
| before 재현 브랜치 | `conn-before` (`5369e46`) — main 과의 차이는 `@Transactional` 1개 |
| 개선 커밋(원본) | `8b6f849` fix: 내부호출로 트랜잭션 호출 안 되는 현상 → ProblemWriter 클래스 추가 |
| 현재 구조 | `ProblemServiceImpl.searchProblem()` — 트랜잭션 없음 / `ProblemWriter.save()` — 문제 단위 트랜잭션 |
