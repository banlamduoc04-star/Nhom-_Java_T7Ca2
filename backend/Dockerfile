# --- Giai doan 1: build jar ---
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# --- Giai doan 2: chay ---
FROM eclipse-temurin:17-jre
WORKDIR /app
# curl cho HEALTHCHECK (NFR-USA-01)
RUN apt-get update && apt-get install -y --no-install-recommends curl     && rm -rf /var/lib/apt/lists/*     && useradd --system --uid 1001 oism
COPY --from=build /app/target/*.jar app.jar
USER oism
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=5     CMD curl -fsS http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
