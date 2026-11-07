# --- Giai doan 1: build jar ---
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# --- Giai doan 2: chay ---
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --uid 1001 oism
COPY --from=build /app/target/*.jar app.jar
USER oism
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
