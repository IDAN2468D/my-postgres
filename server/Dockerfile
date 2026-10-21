# Multi-stage Docker build for Java Spring Boot 3
FROM maven:3.9-eclipse-temurin-17-alpine AS builder
WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY --from=builder /app/target/app.jar ./app.jar

ENV PORT=5001
ENV PGHOST=postgres
ENV PGPORT=5432
ENV PGDATABASE=mydb
ENV PGUSER=postgres
ENV PGPASSWORD=password

EXPOSE 5001

ENTRYPOINT ["java", "-Dfile.encoding=UTF-8", "-jar", "app.jar"]
