# ===== Build stage =====
FROM maven:3.9.9-eclipse-temurin-17 AS builder
WORKDIR /workspace
# Pre-copy pom.xml for dependency caching
COPY pom.xml ./
RUN mvn -q -e -DskipTests dependency:go-offline || true
# Copy sources
COPY src ./src
# Build
RUN mvn -q -DskipTests package

# ===== Runtime stage (Ubuntu base with ffmpeg) =====
FROM eclipse-temurin:17-jre-jammy AS runtime
ENV TZ=UTC
WORKDIR /app

# Install ffmpeg
RUN apt-get update \
    && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends ffmpeg \
    && rm -rf /var/lib/apt/lists/*

# Copy application JAR
COPY --from=builder /workspace/target/*-SNAPSHOT.jar /app/app.jar

# Copy Vosk models into image (makes image large, but fully standalone)
COPY --from=builder /workspace/src/main/resources/model /app/models

# Spring Boot defaults and ports
ENV JAVA_OPTS="" \
    SERVER_PORT=8080 \
    VOSK_LARGE_MODEL_PATH=/app/models/vosk-model-en-us-0.22-lgraph \
    VOSK_SMALL_MODEL_PATH=/app/models/vosk-model-small-en-us-0.15
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]





