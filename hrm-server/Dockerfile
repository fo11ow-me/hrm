# syntax=docker/dockerfile:1
FROM maven:3.8-eclipse-temurin-17 AS maven-builder
WORKDIR /build
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2,id=maven-cache \
    mvn dependency:go-offline -B -q 2>/dev/null || true
COPY src/ src/
RUN --mount=type=cache,target=/root/.m2,id=maven-cache \
    mvn package -DskipTests -B -q

FROM eclipse-temurin:17-jre-jammy
RUN --mount=type=cache,target=/var/cache/apt,sharing=locked \
    --mount=type=cache,target=/var/lib/apt,sharing=locked \
    apt-get update \
    && apt-get install --no-install-recommends -y curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system app \
    && useradd --system --gid app --home-dir /opt/hrm --create-home app

WORKDIR /opt/hrm
COPY --from=maven-builder --chown=app:app /build/target/hrm-server-*.jar app.jar

USER app
EXPOSE 8888

HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=5 \
  CMD curl --fail --silent --show-error http://127.0.0.1:8888/actuator/health > /dev/null || exit 1

ENTRYPOINT ["java", "-Xmx512m", "-jar", "app.jar"]
