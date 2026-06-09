FROM eclipse-temurin:17-jre-jammy AS builder

WORKDIR /workspace
COPY target/hrm-server-*.jar app.jar
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

# 分层复制：依赖层（改 pom.xml 才变）→ 应用层（改代码才变）
COPY --from=builder --chown=app:app /workspace/dependencies/ ./
COPY --from=builder --chown=app:app /workspace/spring-boot-loader/ ./
COPY --from=builder --chown=app:app /workspace/snapshot-dependencies/ ./
COPY --from=builder --chown=app:app /workspace/application/ ./

USER app
EXPOSE 8888

HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=5 \
  CMD curl --fail --silent --show-error http://127.0.0.1:8888/actuator/health > /dev/null || exit 1

ENTRYPOINT ["java", "-Xmx512m", "org.springframework.boot.loader.JarLauncher"]
