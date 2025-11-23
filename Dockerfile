# ===== Whisper native library build stage =====
# Use the SAME base as runtime so GLIBC version matches
FROM eclipse-temurin:17-jre-jammy AS whisper-build

ARG WHISPER_COMMIT=master

RUN apt-get update && \
    apt-get install -y --no-install-recommends \
      git \
      ca-certificates \
      build-essential \
      cmake && \
    update-ca-certificates && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /opt

# Clone whisper.cpp (cached as its own layer)
RUN git clone --depth 1 https://github.com/ggml-org/whisper.cpp.git

WORKDIR /opt/whisper.cpp

# If you want a specific commit instead of latest, you can do:
# RUN git fetch --depth 1 origin ${WHISPER_COMMIT} && git checkout ${WHISPER_COMMIT}

# Configure, build and install the shared library
RUN cmake -B build -DCMAKE_BUILD_TYPE=Release \
 && cmake --build build -j"$(nproc)" \
 && cmake --install build --prefix /opt/whisper


# ===== Build stage (Maven) =====
FROM maven:3.9.9-eclipse-temurin-17 AS builder
WORKDIR /workspace

# Pre-copy pom.xml for dependency caching
COPY pom.xml ./
RUN mvn -q -e -DskipTests dependency:go-offline || true

# Copy sources
COPY src ./src

# Build the jar
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

# Copy whisper native libs built in whisper-build stage
COPY --from=whisper-build /opt/whisper /opt/whisper

# Environment for Spring Boot + JNA
ENV JAVA_OPTS="" \
    SERVER_PORT=8080 \
    JNA_LIBRARY_PATH=/opt/whisper/lib \
    LD_LIBRARY_PATH=/opt/whisper/lib:$LD_LIBRARY_PATH

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
