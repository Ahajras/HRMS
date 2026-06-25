# syntax=docker/dockerfile:1

# ---- Build stage: compile and package the Spring Boot jar (JDK 21) ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Cache dependencies first (only re-downloads when pom.xml changes)
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# Build the application
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---- Run stage: slim JRE 21 ----
FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app

# Non-root user
RUN groupadd --system hrms && useradd --system --gid hrms hrms

COPY --from=build /app/target/hrms-backend.jar app.jar
RUN chown -R hrms:hrms /app
USER hrms

EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"

# Actuator health endpoint used by compose healthcheck
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=10 \
  CMD ["sh", "-c", "wget -qO- http://localhost:8080/actuator/health | grep -q UP || exit 1"]

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
