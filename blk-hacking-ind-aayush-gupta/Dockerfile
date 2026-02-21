# docker build -t blk-hacking-ind-aayush-gupta .
# This Dockerfile builds a containerized Spring Boot application for the BlackRock Retirement Challenge
# Alpine Linux is chosen as the base OS due to its minimal security footprint (~5MB) and reduced attack surface

FROM eclipse-temurin:21-jdk-alpine

# Alpine Linux is used for its security hardening, minimal size, and production-grade stability.
# Security benefits: Minimal packages = minimal vulnerabilities. Package management: apk (secure).
# Size: ~100MB base image vs ~600MB with standard JDK. Ideal for cloud deployments and microservices.

# Set working directory inside container
WORKDIR /app

# Copy the built JAR file from the local target directory to the container
# The JAR is built locally with: mvn clean package -DskipTests
COPY target/blk-hacking-ind-aayush-gupta-0.0.1-SNAPSHOT.jar app.jar

# Expose the port where the Spring Boot application listens
EXPOSE 5477

# Health check to monitor container health every 30 seconds
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:5477/blackrock/challenge/v1/performance || exit 1

# Set the entry point to run the Spring Boot application
# The Java process runs with minimal heap (500MB) and enables GC logging for monitoring
ENTRYPOINT ["java", "-Xmx512m", "-Xms256m", "-jar", "app.jar"]

# Docker usage:
# Build:   docker build -t blk-hacking-ind-aayush-gupta .
# Run:     docker run -d -p 5477:5477 --name blackrock-app blk-hacking-ind-aayush-gupta
# Logs:    docker logs -f blackrock-app
# Stop:    docker stop blackrock-app
