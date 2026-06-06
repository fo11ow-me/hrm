#!/usr/bin/env bash
set -Eeuo pipefail

DEPLOY_DIR="${DEPLOY_DIR:-$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)}"
COMPOSE_FILE="${DEPLOY_DIR}/docker-compose.prod.yml"
ENV_FILE="${DEPLOY_DIR}/.env"
RELEASE_FILE="${DEPLOY_DIR}/release.env"
NEW_TAG="${1:-}"

if [[ -z "${NEW_TAG}" ]]; then
  echo "usage: deploy.sh <image-tag>" >&2
  exit 2
fi

if [[ ! "${NEW_TAG}" =~ ^sha-[0-9a-f]{40}$ ]]; then
  echo "image tag must match sha-<40 lowercase hex characters>" >&2
  exit 2
fi

if [[ ! -f "${ENV_FILE}" ]]; then
  echo "server .env is missing" >&2
  exit 2
fi

if [[ ! -f "${COMPOSE_FILE}" ]]; then
  echo "docker-compose.prod.yml is missing" >&2
  exit 2
fi

umask 077
chmod 600 "${ENV_FILE}"

read_release_tag() {
  if [[ -f "${RELEASE_FILE}" ]]; then
    sed -n 's/^IMAGE_TAG=//p' "${RELEASE_FILE}" | tail -n 1
  fi
}

read_env_value() {
  local key="$1"
  local default_value="$2"
  local value
  value="$(sed -n "s/^${key}=//p" "${ENV_FILE}" | tail -n 1)"
  printf '%s' "${value:-${default_value}}"
}

write_release_tag() {
  printf 'IMAGE_TAG=%s\n' "$1" > "${RELEASE_FILE}"
  chmod 600 "${RELEASE_FILE}"
}

compose() {
  docker compose \
    --env-file "${ENV_FILE}" \
    --env-file "${RELEASE_FILE}" \
    -f "${COMPOSE_FILE}" \
    "$@"
}

wait_healthy() {
  local service="$1"
  local deadline=$((SECONDS + 180))
  local container_id
  local health

  while (( SECONDS < deadline )); do
    container_id="$(compose ps -q "${service}")"
    if [[ -n "${container_id}" ]]; then
      health="$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "${container_id}")"
      if [[ "${health}" == "healthy" ]]; then
        return 0
      fi
      if [[ "${health}" == "unhealthy" || "${health}" == "exited" || "${health}" == "dead" ]]; then
        return 1
      fi
    fi
    sleep 5
  done

  return 1
}

verify_local_endpoints() {
  local backend_port
  local frontend_port
  backend_port="$(read_env_value BACKEND_BIND_PORT 18888)"
  frontend_port="$(read_env_value FRONTEND_BIND_PORT 18080)"

  curl --fail --silent --show-error --output /dev/null "http://127.0.0.1:${backend_port}/actuator/health"
  curl --fail --silent --show-error --output /dev/null "http://127.0.0.1:${frontend_port}/healthz"
}

verify_public_endpoint() {
  local public_healthcheck_url
  public_healthcheck_url="$(read_env_value PUBLIC_HEALTHCHECK_URL '')"
  if [[ -z "${public_healthcheck_url}" ]]; then
    echo "PUBLIC_HEALTHCHECK_URL is missing" >&2
    return 1
  fi
  if ! curl --fail --silent --output /dev/null "${public_healthcheck_url}"; then
    echo "public health check failed" >&2
    return 1
  fi
}

PREVIOUS_TAG="$(read_release_tag)"

rollback() {
  trap - ERR
  if [[ "${PREVIOUS_TAG}" =~ ^sha-[0-9a-f]{40}$ && "${PREVIOUS_TAG}" != "${NEW_TAG}" ]]; then
    echo "deployment failed; restoring previous release" >&2
    write_release_tag "${PREVIOUS_TAG}"
    compose pull >/dev/null 2>&1 || true
    compose up -d --remove-orphans >/dev/null 2>&1 || true
    wait_healthy hrm-server >/dev/null 2>&1 || true
    wait_healthy hrm-admin >/dev/null 2>&1 || true
    verify_local_endpoints >/dev/null 2>&1 || true
  elif [[ -z "${PREVIOUS_TAG}" ]]; then
    rm -f "${RELEASE_FILE}"
  fi
  exit 1
}

trap rollback ERR

write_release_tag "${NEW_TAG}"
compose config --quiet
compose pull >/dev/null
compose up -d --remove-orphans >/dev/null
wait_healthy hrm-server
wait_healthy hrm-admin
verify_local_endpoints
verify_public_endpoint

trap - ERR
docker image prune -f --filter "until=168h" >/dev/null 2>&1 || true
echo "deployment completed"
