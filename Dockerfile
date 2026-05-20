# =============================================================================
# Dockerfile — Multi-stage build for gitops-app
#
# WHY MULTI-STAGE?
# A single-stage build would leave the final image bloated with Maven,
# the JDK compiler, source code, and the entire Maven local repository
# (~500MB+). Multi-stage builds solve this by separating the build
# environment from the runtime environment.
#
# Stage 1 (builder): Has everything needed to COMPILE the app.
# Stage 2 (runtime): Has only what is needed to RUN the app.
# The final image contains ONLY the compiled JAR + a lean JRE.
# This directly reduces attack surface and image pull time in your pipeline.
# =============================================================================


# =============================================================================
# STAGE 1 — BUILD STAGE
# =============================================================================

# eclipse-temurin is the official, production-grade OpenJDK distribution
# maintained by the Eclipse Adoptium project. It is the recommended JDK
# image for Docker — preferred over openjdk (deprecated on Docker Hub)
# and over vendor-specific images for portability.
#
# We use the JDK (not JRE) here because Maven needs the full JDK to compile.
# 'alpine' variant keeps the builder image smaller, speeding up CI layer caching.
FROM eclipse-temurin:17-jdk-alpine AS builder

# Set a clean working directory inside the builder container.
# All subsequent COPY and RUN commands in this stage operate relative to this.
WORKDIR /build

# -----------------------------------------------------------------------------
# COPY DEPENDENCY MANIFEST FIRST — Docker layer cache optimisation
#
# We copy pom.xml and download dependencies BEFORE copying source code.
# Why: Docker caches each layer. If source code changes but pom.xml does not,
# the 'mvn dependency:go-offline' layer is served from cache — skipping a
# potentially multi-minute Maven download on every build.
# This is one of the most impactful Dockerfile optimisations for Java apps.
# -----------------------------------------------------------------------------
COPY pom.xml .

# Download all dependencies declared in pom.xml into the local Maven cache.
# '-B' = batch mode (no interactive prompts, clean CI output).
# 'dependency:go-offline' resolves and caches every dependency and plugin
# so the subsequent build step has no network dependency.
RUN mvn -B dependency:go-offline

# Now copy the full source tree. This layer will invalidate (rebuild) on
# any source code change, but the dependency cache layer above remains intact.
COPY src ./src

# Package the application into an executable fat JAR.
# '-DskipTests=false' is the default — tests run here intentionally.
# In the Jenkinsfile, tests run as a separate stage before Docker build,
# so we skip them here to avoid running them twice in the pipeline.
# Adjust to '-DskipTests=false' if you want Docker build to also verify tests.
RUN mvn -B package -DskipTests=true

# =============================================================================
# STAGE 2 — RUNTIME STAGE
# =============================================================================

# JRE (not JDK) for runtime — the compiler toolchain is not needed to RUN
# the app, only to build it. The JRE is significantly smaller than the JDK.
# 'jammy' = Ubuntu 22.04 LTS base. Chosen over alpine for runtime because:
#   - Better glibc compatibility (some native libs fail on Alpine's musl libc)
#   - More familiar for debugging (apt-get available if needed)
#   - Still significantly smaller than the JDK builder stage
FROM eclipse-temurin:17-jre-jammy

# Metadata labels — OCI image spec standard.
# These appear in 'docker inspect' and are indexed by registries like Docker Hub.
# In a CI pipeline, inject these dynamically:
#   --label org.opencontainers.image.revision=$GIT_COMMIT
LABEL org.opencontainers.image.title="gitops-app" \
      org.opencontainers.image.description="Spring Boot app for GitOps portfolio" \
      org.opencontainers.image.source="https://github.com/YOUR_USERNAME/gitops-app"

WORKDIR /app

# -----------------------------------------------------------------------------
# NON-ROOT USER — Security hardening
#
# By default, Docker containers run as root (UID 0). This means a container
# escape vulnerability gives an attacker root on the host. Running as a
# dedicated non-root user limits the blast radius.
#
# Best practice:
#   1. Create a system group and user with no login shell and no home dir
#   2. chown the app directory to that user
#   3. Switch to that user with USER before the entrypoint
#
# In Kubernetes, this pairs with:
#   securityContext:
#     runAsNonRoot: true
#     runAsUser: 1001
# -----------------------------------------------------------------------------
RUN groupadd --system --gid 1001 appgroup && \
    useradd --system --uid 1001 --gid appgroup --no-create-home appuser

# Copy ONLY the compiled fat JAR from the builder stage.
# Nothing else from Stage 1 (no source, no Maven cache, no JDK) is copied.
# This is the core of the multi-stage build — a clean, minimal final image.
COPY --from=builder /build/target/*.jar app.jar

# Give the non-root user ownership of the JAR.
RUN chown appuser:appgroup app.jar

# Switch to the non-root user for all subsequent instructions
# (including CMD/ENTRYPOINT). The container process will run as UID 1001.
USER appuser

# Document that the container listens on port 8080.
# EXPOSE is documentation only — it does not publish the port.
# Actual port binding happens at 'docker run -p 8080:8080' or in K8s
# under containerPort. Must match server.port in application.properties.
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