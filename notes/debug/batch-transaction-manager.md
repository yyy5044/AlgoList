# 배치 메타데이터 트랜잭션 매니저 지정 누락

**문제 상황**: `CodeforcesIngestJobConfig.java`를 보던 중, JobRepository에 등록된 트랜잭션 매니저가 batch_system DB가 아니라 algolist DB의 매니저인 것을 발견했다. 이 상태에서는 JobRepository가 DB 작업을 해도 트랜잭션으로 묶이지 않는다. 지금까지는 SQL을 하나씩 따로 실행해도 문제없는 SQL만 날렸기 때문에 겉으로 드러나지 않았다.

**수정한 파일**: `backend/src/main/java/com/algolist/backend/db/BatchSystemDBConfig.java`

## 원래 코드

```java
@Bean
PlatformTransactionManager batchSystemTransactionManager(...) {...}
```

## 수정 코드

```java
import org.springframework.boot.batch.autoconfigure.BatchTransactionManager;

@Bean
@BatchTransactionManager
PlatformTransactionManager batchSystemTransactionManager(...) {...}
```

Spring Boot는 `@BatchTransactionManager`가 붙은 빈이 있으면 그것을, 없으면 `@Primary` 매니저를 JobRepository에 넣는다. 데이터소스와 매니저는 짝으로 지정해야 한다.
