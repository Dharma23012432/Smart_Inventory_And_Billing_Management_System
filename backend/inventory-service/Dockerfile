# =====================================================================
# Stage 1: Build Stage using Maven and Eclipse Temurin JDK 21
# =====================================================================
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# Copy Maven POM and source code
COPY pom.xml .
COPY src ./src

# Build production JAR skipping unit tests (tests are verified in CI/CD pipeline)
RUN mvn clean package -DskipTests

# =====================================================================
# Stage 2: Runtime Stage using lightweight Eclipse Temurin JRE 21
# =====================================================================
FROM eclipse-temurin:21-jre-alpine AS runner

WORKDIR /app

# Create a dedicated non-root user and group for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy compiled JAR from builder stage
COPY --from=builder --chown=appuser:appgroup /build/target/*.jar app.jar

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
