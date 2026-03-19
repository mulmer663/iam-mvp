# Multi-stage build for Spring Boot application
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app
COPY . .
RUN chmod +x ./gradlew
RUN ./gradlew :iam-engine:build -x test

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/iam-engine/build/libs/iam-engine-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
