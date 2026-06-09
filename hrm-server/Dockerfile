FROM eclipse-temurin:17-jre-jammy

RUN --mount=type=cache,target=/var/cache/apt,sharing=locked \
    --mount=type=cache,target=/var/lib/apt,sharing=locked \
    apt-get update \
    && apt-get install --no-install-recommends -y curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system app \
    && useradd --system --gid app --home-dir /opt/hrm --create-home app

WORKDIR /opt/hrm
COPY --chown=app:app target/hrm-server-*.jar app.jar

USER app
EXPOSE 8888

HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=5 \
  CMD curl --fail --silent --show-error http://127.0.0.1:8888/actuator/health > /dev/null || exit 1

ENTRYPOINT ["java", "-Xmx512m", "-jar", "/opt/hrm/app.jar"]
