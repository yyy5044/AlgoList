# 수업 이어하기 안내 (다른 컴퓨터 / 새 세션용)

마지막 갱신: 2026-10-07

## 새 세션에서 처음에 보낼 메시지

아래를 그대로 붙여 넣고, 이 파일과 `codeforces-batch-oom-study-plan.md` 두 개를 함께 첨부한다.

```
첨부한 두 파일을 읽어줘. lesson-handoff.md에 적힌 수업 방식과 진행 상황대로,
선생님 역할로 "다음 할 일"부터 이어서 진행해줘. 수치와 경과는 study-plan.md 1절에 있는 것만 써줘.
코드는 github.com/yyy5044/AlgoList 의 main 브랜치 기준이야.
```

## 수업 방식 (선생님 역할 규칙)

- 학생은 노트와 펜을 준비한 상태다. "받아 적기", "그리기", "생각해볼 질문"으로 지시한다.
- 한 번에 한 교시만. 짧게. 교시마다 핵심 문장 하나에 밑줄을 치게 한다.
- 교시 끝에 질문을 하나만 낸다. 학생의 답을 보고 보충한 뒤 다음으로 넘어간다.
- 초등학생도 이해할 비유를 먼저 쓰고, 용어는 그 다음에 붙인다.
- **설명하기 전에 반드시 코드를 다 읽는다.** 읽기 전에 설명해서 정정을 여러 번 한 적이 있고 학생이 불쾌해했다.
- **학생이 한 것과 Claude가 한 것을 구분해서 말한다.** Claude가 한 일을 학생이 면접에서 자기 것처럼 말하게 만들면 안 된다.
- 지금까지 쓴 비유: 가벼운 행은 "깃털", 107MB짜리 행은 "바위". Reader는 "창고에서 박스째 꺼내와 한 병씩 건네는 편의점 알바". Spring 컨테이너는 "창고", 빈은 "창고에 등록된 비품". 트랜잭션 매니저는 "자기 가게 문만 관리하는 점원".
- 최종 목표는 면접에서 꼬리질문 두세 단계까지 본인 말로 답하는 것이다.
- 사실이 아닌 것은 말하게 두지 않는다. study-plan.md 1.3절 "말하면 안 되는 것"을 지킨다.

## 진행 상황

### 끝난 수업

**1교시: 이야기의 뼈대와 데이터 모양**
- 전체 이야기 다섯 문장. 응답 구조 `{ rows: [ { row_idx, row: {27개 필드} } x100 ], num_rows_total }`.
- 566번 행이 107.4MB, 그중 쓰는 10개 필드는 1.8KB. 대부분은 깃털, 가끔 바위.
- 학생 답(정답): "엄청 큰 official_tests 하나만 만나도 터지니까".

**2교시: Spring Batch의 세 일꾼과 chunk**
- Reader 한 건씩, Processor 변환, Writer 묶음 저장. chunk 100 = 한 바퀴 = 트랜잭션 하나.
- 학생 답(정답): "리더가 먼저 메모리에 올려야 하는데 그때 바위 같은 행을 올리니까". 정리: 버리는 시점이 너무 늦었다.

**3교시: 힙과 상한**
- 힙 상한은 직접 지정 없으면 물리 메모리의 1/4. t3.micro 약 250MiB, t3.small 약 500MiB. 코드는 같고 상한만 달랐다.
- swap 정의까지 설명. **질문 "swap 2GiB 추가했는데 왜 효과 없었나"는 아직 학생이 답하지 않음.**

**설계도 수업 (CodeforcesIngestJobConfig 읽기, 여러 턴)**
- 학생이 Spring Batch와 Spring 자체(빈, @Component, @Bean)를 거의 몰라서 기초부터 함.
- 다룬 것: @Configuration/@Bean = 조립 설명서, 매개변수 8개는 부품 목록이고 Spring이 타입으로 창고에서 꺼내 넣음, 파일 안 순서는 의미 없음, @Component(클래스)와 @Bean(메서드) 둘 다 빈, 빈 = Spring이 만들고 하나만 두고 주입하고 관리하는 객체, 매개변수 타입이 인터페이스여도 구현체가 들어옴, ItemReader/ItemWriter는 규격이고 Reader/Writer는 직접 만든 구현체, Parser는 ItemProcessor 대신 자작 함수형 인터페이스 ProblemParser를 구현해 `parser::parse`로 꽂음, Writer는 common 폴더(두 배치 공용).
- JobRepository = Spring Batch가 자동으로 만든 메타데이터 Repository(배치 장부). 데이터소스는 통로, JobRepository는 그 통로로 장부를 적는 직원.
- 트랜잭션 매니저: 데이터소스 하나에 묶여 그 연결만 관리. DB 둘이라 매니저 둘(둘 다 직접 만듦, algolist 것이 @Primary). Step의 txManager는 algolist(청크가 algolist에 쓰므로 맞음). 매니저가 엉뚱하면 SQL은 실행되되 묶이지 않고 문장마다 자동 커밋.
- Spring Batch 6에서 Step의 트랜잭션 매니저는 문법상 선택(기본 ResourcelessTransactionManager)이지만 DB에 쓰는 Step이면 실질 필수.
- 파서 구조 평가: 예외 던져 skipLimit에 태운 건 옳지만, 구현체 하나짜리 인터페이스와 배치마다 다른 실패 처리(코드포스 예외/skip, 백준 null/filter)는 어색. 통일이 맞다고 학생이 먼저 말하기로.

**학생이 직접 한 수정 (미커밋, main 작업트리)**
- `BatchSystemDBConfig.batchSystemTransactionManager`에 `@BatchTransactionManager` 추가. 이유: JobRepository에 algolist 매니저가 붙어 있었음(`@BatchDataSource`만 있고 매니저 표시가 없어 Boot가 @Primary로 폴백). 단일 서버라 증상은 없었음. 면접에서는 "코드 다시 보다 알게 돼 수정"으로만 말하기로 함. 과장 금지.
- `CodeforcesIngestJobConfig`의 txManager 주석 수정("자동 구성, 유일"은 틀린 주석이었음).
- `ProblemItemWriter.java`에 실수로 공백 변경이 섞여 있음. 되돌리라고 안내했으나 아직 안 됨.
- Claude가 전후 검증 테스트(`BatchTxManagerWiringTest`, git 제외)를 만들어 돌림: 수정 전 algolist 매니저, 수정 후 batch_system 매니저. **이건 Claude가 한 것.**
- 기록: `notes/debug/batch-transaction-manager.md` (저장소에 올라갈 파일).
- 디버깅 기록 규칙을 스킬로 만듦: `.claude/skills/debug-note/SKILL.md`. 핵심은 "수정한 파일" 표기와 "접기 규칙"(변경 지점만 펼치고 `(...)`, `{...}`로 접기, 양쪽 같은 모양).

### 다음 할 일 (이 순서로)

1. 미커밋 정리: Writer 공백 되돌리기, 브랜치 만들어 두 파일 + notes/debug + 스킬 커밋. 커밋 메시지 본문에 "왜"를 학생 본인 문장으로 쓰게 하기(과제).
2. 3교시 남은 질문 답 받기: swap이 왜 효과 없었나. (기대 답: 힙 상한은 물리 메모리 기준이라 swap으로 안 늘어남)
3. **Reader 읽기 가이드 진행.** 가이드는 이미 줬음(아래 "읽기 가이드 요약"). 학생이 읽고 답을 보내면 보충. 아직 시작 안 함.
4. Parser, Writer 읽기 → 백준 배치(GitHubIngestJobConfig, GitHubProblemReader, GitHubProblemProcessor) 읽기 → 두 배치 차이 표 그리기.
5. 이후 study-plan.md의 남은 주제: D(수정 전 메모리 해부), E(스트림 파싱 코드), F(원인 추적과 대안), G(측정), I, J, 모의 면접.
6. 고칠 것 후보(학생이 결정): 파서 구조 통일(ProblemParser를 백준에도 적용, 실패는 예외로), 백준 Reader fileIndex 체크포인트, 오래된 주석(page-size 20, 무거운 필드 이름).

### 읽기 가이드 요약 (Reader)

`CodeforcesPageReader.java`를 이 순서로: 필드(48~77) → open(94) → read(106) → nextAvailablePage(127) → update(160) → fetchPage(169)·requestWithRetry(186, 190번 exchange) → parseRows(220) → extractSlimRow(256, 268과 270의 차이).
답할 질문: API 호출은 몇 건마다 / currentPage가 Iterator인 이유 / 페이지 실패 시 nextOffset / KEEP에 없는 필드는 어떤 메서드로 어떻게 되나 / 268의 readTree와 수정 전 readTree의 차이 / @StepScope 빼면 / 슬림 노드 100건은 어디에.
학생이 보낼 것: read() 흐름도 말로 옮긴 것, "Processor는 어디 있나" 한 문장, 이해 안 된 줄 번호.

## 이 파일들에 없는 것

- 이 컴퓨터의 Claude 메모리와 이전 대화 내용은 따라가지 않는다. 사실은 study-plan.md 1절, 진행 상황은 이 파일에 있다.
- 다른 면접 답변(배치 재시작 설계, 동시성 락, 트랜잭션 분리, JobUp 동시 신청 테스트)은 두 파일에 없다.
- `docs/interview/`는 git 제외 대상이라 main에 없고 `interview-notes` 브랜치에만 있다.
- 측정 테스트와 before/after 코드는 GitHub의 `measurement`, `perf-before`, `perf-after` 브랜치에 있다.
- 로컬 DB: AlgoList 전용 MySQL 없음. 이 컴퓨터에서는 JobUp 컨테이너(3306, root/ssafy)에 algolist·batch_system 스키마를 넣어 썼음. 집 컴퓨터는 별도 준비 필요.
