# syntax=docker/dockerfile:1

# ---- Stage 1: build ----
# Uses a Maven + JDK 21 image to produce the executable jar. Dependencies are
# resolved in a separate layer so they are cached unless pom.xml changes.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml .
# Cache warming only: Maven Central/plugin metadata can fail transiently even
# when the authoritative package build can resolve normally. Keep this layer
# best-effort, but never suppress a failure from the package command below.
RUN --mount=type=cache,id=sami-maven-m2,target=/root/.m2,sharing=locked \
    if ! mvn -q -U -B \
      -Dmaven.wagon.http.retryHandler.count=3 \
      -Dmaven.resolver.transport.http.retryHandler.count=5 \
      -Daether.connector.http.retryHandler.count=5 \
      -Dmaven.wagon.rto=30000 \
      dependency:go-offline; then \
        echo "Maven dependency warm-up failed; continuing to authoritative package build."; \
    fi

COPY src ./src
ARG BUILD_BRANCH=unknown
ARG BUILD_COMMIT=unknown
# Maven Resolver may see transient connection resets from a remote repository.
# Retrying the complete authoritative package command lets its shared cache
# retain successful downloads, but the third failure exits the image build.
RUN --mount=type=cache,id=sami-maven-m2,target=/root/.m2,sharing=locked \
    for attempt in 1 2 3; do \
      if mvn -q -U clean package -DskipTests -B \
        -Dmaven.wagon.http.retryHandler.count=3 \
        -Dmaven.resolver.transport.http.retryHandler.count=5 \
        -Daether.connector.http.retryHandler.count=5 \
        -Dmaven.wagon.rto=30000 \
        -Dbuild.branch="$BUILD_BRANCH" -Dbuild.commit="$BUILD_COMMIT"; then break; fi; \
      if [ "$attempt" = 3 ]; then exit 1; fi; \
      echo "Authoritative Maven package failed; retrying ($attempt/3)..."; sleep 10; \
    done

# ---- Stage 2: runtime ----
# Minimal JRE image, non-root user, only the jar is carried over.
FROM eclipse-temurin:21-jre-jammy AS runtime
ARG BUILD_COMMIT=unknown
ARG APP_VERSION=0.1.0
LABEL org.opencontainers.image.revision=$BUILD_COMMIT \
      org.opencontainers.image.version=$APP_VERSION
WORKDIR /app

# The Temurin JRE image already provides curl for the health check. Avoid an
# unnecessary apt transaction so runtime image creation stays independent of
# transient Ubuntu archive availability.
# Run as an unprivileged user. The uploads directory is created here so the
# named volume mounted over it inherits spring's ownership on first use.
RUN groupadd --system spring && useradd --system --gid spring spring \
    && mkdir -p /app/data/uploads /app/data/files /app/data/files-staging \
    && chown -R spring:spring /app/data
USER spring:spring

COPY --from=build /workspace/target/*.jar app.jar

EXPOSE 8080
ENV JAVA_OPTS=""
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=5 \
    CMD curl --fail --silent http://localhost:8080/actuator/health >/dev/null || exit 1
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
