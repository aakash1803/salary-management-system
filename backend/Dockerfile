# Stage 1: Build stage with Java 21 JDK
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Copy Gradle wrapper and configuration files for dependency caching
COPY gradlew build.gradle settings.gradle ./
COPY gradle/ gradle/

RUN chmod +x gradlew
RUN ./gradlew dependencies --no-daemon || true

# Copy backend source code
COPY src/ src/

# Build production Spring Boot executable JAR
RUN ./gradlew bootJar -x test --no-daemon

# Stage 2: Seeder target stage (runs DatabaseSeederApplication directly via Java 21 JRE, without Gradle runtime)
FROM eclipse-temurin:21-jre-alpine AS seeder
WORKDIR /app

RUN addgroup -S spring -g 1000 && adduser -S spring -u 1000 -G spring
RUN mkdir -p /app/data && chown -R spring:spring /app
USER spring:spring

COPY --from=builder --chown=spring:spring /app/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-cp", "app.jar", "-Dloader.main=com.salarymanagement.seed.DatabaseSeederApplication", "org.springframework.boot.loader.launch.PropertiesLauncher"]

# Stage 3: Production Runtime with Java 21 JRE
FROM eclipse-temurin:21-jre-alpine AS runner
WORKDIR /app

RUN addgroup -S spring -g 1000 && adduser -S spring -u 1000 -G spring
RUN mkdir -p /app/data && chown -R spring:spring /app

USER spring:spring

COPY --from=builder --chown=spring:spring /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
