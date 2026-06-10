# syntax=docker/dockerfile:1
FROM maven:3.8-eclipse-temurin-17 AS maven-builder
WORKDIR /build

# 分层复制：先复制 pom.xml 单独下载依赖（利用 Docker 层缓存）
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2,id=maven-cache \
    mvn dependency:go-offline -B -q 2>/dev/null || true

# 复制源码编译（Maven 缓存挂载复用，依赖无需重新下载）
COPY src/ src/
RUN --mount=type=cache,target=/root/.m2,id=maven-cache \
    mvn package -DskipTests -B -q

FROM eclipse-temurin:17-jre-jammy AS builder
WORKDIR /workspace
COPY --from=maven-builder /build/target/hrm-server-*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

FROM eclipse-temurin:17-jre-jammy

RUN --mount=type=cache,target=/var/cache/apt,sharing=locked \
    --mount=type=cache,target=/var/lib/apt,sharing=locked \
    apt-get update \
    && apt-get install --no-install-recommends -y curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system app \
    && useradd --system --gid app --home-dir /opt/hrm --create-home app

WORKDIR /opt/hrm

COPY --from=builder --chown=app:app /workspace/dependencies/ ./
COPY --from=builder --chown=app:app /workspace/spring-boot-loader/ ./
COPY --from=builder --chown=app:app /workspace/snapshot-dependencies/ ./
COPY --from=builder --chown=app:app /workspace/application/ ./

USER app
EXPOSE 8888

HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=5 \
  CMD curl --fail --silent --show-error http://127.0.0.1:8888/actuator/health > /dev/null || exit 1

ENTRYPOINT ["java", "-Xmx512m", "org.springframework.boot.loader.JarLauncher"]
