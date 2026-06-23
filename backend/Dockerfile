# ===== 1단계(build): Maven으로 jar 빌드 (JDK 필요, 무거움) =====
FROM eclipse-temurin:21-jdk AS  build
WORKDIR /app

# 의존성 캐싱 트릭: pom과 wrapper를 먼저 복사해 의존성만 받아둔다.
# 이러면 src만 바뀌었을 때 아래 무거운 의존성 다운로드 레이어를 재사용 -> 빌드 속도 빨라짐
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
# Windows에서 커밋되면 실행 비트(+x)가 유실되므로 컨테이너 안에서 직접 부여
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# 소스 복사 후 jar 빌드 (-DskipTests: 빌드 땐 테스트 생략)
COPY src ./src
RUN ./mvnw clean package -DskipTests -B

# ===== 2단계(run): jar만 실행 (JRE만, 가벼움) =====
FROM eclipse-temurin:21-jre
WORKDIR /app

# 1단계에서 만든 jar만 가져온다 (빌드 도구와 소스는 안 가져옴 -> 이미지 슬림하게)
COPY --from=build /app/target/backend-0.0.1-SNAPSHOT.jar app.jar

# Spring Boot 기본 포트
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]