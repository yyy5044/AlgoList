FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY . .
RUN ./mvnw package -DskipTests
ENTRYPOINT ["java", "-jar", "target/backend-0.0.1-SNAPSHOT.jar"]