# syntax=docker/dockerfile:1
FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY src src
RUN --mount=type=cache,target=/root/.m2 mvn -B -Dmaven.test.skip=true package

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S -g 10003 leoms && adduser -S -D -H -u 10003 -G leoms leoms
WORKDIR /app
COPY --from=build /build/target/leoms-admin-*.jar app.jar
USER 10003:10003
EXPOSE 8080 9090
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=70", "-jar", "/app/app.jar"]
