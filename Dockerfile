# syntax=docker/dockerfile:1

# ---- Stage 1: build ----
# Uses a Maven + JDK 21 image to produce the executable jar. Dependencies are
# resolved in a separate layer so they are cached unless pom.xml changes.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml .
RUN mvn -q dependency:go-offline -B

COPY src ./src
RUN mvn -q clean package -DskipTests -B

# ---- Stage 2: runtime ----
# Minimal JRE image, non-root user, only the jar is carried over.
FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app

# Run as an unprivileged user. The uploads directory is created here so the
# named volume mounted over it inherits spring's ownership on first use.
RUN groupadd --system spring && useradd --system --gid spring spring \
    && mkdir -p /app/data/uploads && chown -R spring:spring /app/data
USER spring:spring

COPY --from=build /workspace/target/*.jar app.jar

EXPOSE 8080
ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
