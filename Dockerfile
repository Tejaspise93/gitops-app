# =============================================================================
# STAGE 1 — BUILD STAGE
# =============================================================================
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build


COPY pom.xml .

RUN mvn -B dependency:go-offline

COPY src ./src

RUN mvn -B package -DskipTests=true

# =============================================================================
# STAGE 2 — RUNTIME STAGE
# =============================================================================

FROM eclipse-temurin:17-jre-jammy


LABEL org.opencontainers.image.title="gitops-app" \
      org.opencontainers.image.description="Spring Boot app for GitOps portfolio" \
      org.opencontainers.image.source="https://github.com/YOUR_USERNAME/gitops-app"

WORKDIR /app

RUN groupadd --system --gid 1001 appgroup && \
    useradd --system --uid 1001 --gid appgroup --no-create-home appuser

COPY --from=builder /build/target/*.jar app.jar

RUN chown appuser:appgroup app.jar

USER appuser

EXPOSE 8080

# -----------------------------------------------------------------------------
# JVM FLAGS — Production tuning
#
# -XX:+UseContainerSupport (default ON in JDK 11+, listed for explicitness):
#   Makes the JVM read cgroup memory/CPU limits instead of host resources.
#   Without this (older JDKs), the JVM would see host RAM (e.g., 64GB) and
#   set heap accordingly — causing OOMKilled pods in Kubernetes.
#
# -XX:MaxRAMPercentage=75.0:
#   Sets max heap to 75% of the container's memory limit.
#   Leaves 25% for the JVM's off-heap (metaspace, thread stacks, native mem).
#   Example: 512Mi container limit → ~384Mi max heap.
#   Never hardcode -Xmx in containers — it ignores cgroup limits.
#
# -Djava.security.egd=file:/dev/./urandom:
#   Fixes slow startup on some Linux environments where the JVM blocks
#   waiting for /dev/random entropy. /dev/urandom is non-blocking and
#   cryptographically sufficient for session token generation.
# -----------------------------------------------------------------------------
ENTRYPOINT ["java", \
            "-XX:+UseContainerSupport", \
            "-XX:MaxRAMPercentage=75.0", \
            "-Djava.security.egd=file:/dev/./urandom", \
            "-jar", \
            "app.jar"]