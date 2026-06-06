#!/usr/bin/env bash
set -Eeuo pipefail

failures=0

report() {
  echo "::error::$1"
  failures=$((failures + 1))
}

tracked_files="$(git ls-files)"

while IFS= read -r file; do
  [[ -z "${file}" ]] && continue
  report "forbidden environment file is tracked: ${file}"
done < <(printf '%s\n' "${tracked_files}" | grep -E '(^|/)\.env($|\.)' | grep -vE '\.example$' || true)

while IFS= read -r file; do
  [[ -z "${file}" ]] && continue
  report "forbidden sensitive or generated file is tracked: ${file}"
done < <(printf '%s\n' "${tracked_files}" | grep -E '(^|/)ssl/|(^|/)remote/|\.key$|\.pem$|\.p12$|\.pfx$|\.jks$|\.tar(\.gz)?$|\.zip$|(^|/)application-(dev|prod)\.yml$|(^|/)(hrm|hrm_activiti|db)\.sql$|scripts/verify-server-file-flows\.ps1$' || true)

while IFS= read -r file; do
  [[ -z "${file}" ]] && continue
  [[ "${file}" == "deploy/ci/init-test-db.sql" ]] && continue
  [[ "${file}" == hrm-server/src/test/resources/sql/*.sql ]] && continue
  report "forbidden database export or snapshot is tracked: ${file}"
done < <(printf '%s\n' "${tracked_files}" | grep -E '\.sql$|\.rdb$' || true)

if git grep -n -I -E 'BEGIN [A-Z ]*PRIVATE KEY|dockerHost>|Source Host[[:space:]]*:' -- . ':!scripts/ci-security-scan.sh'; then
  report "private key material or hard-coded infrastructure metadata detected"
fi

if git grep -n -I -i -E 'String[[:space:]]+[A-Za-z0-9_]*(secret|password|token|key)[A-Za-z0-9_]*[[:space:]]*=[[:space:]]*"[A-Za-z0-9+/=_-]{16,}"[[:space:]]*;' -- . ':!scripts/ci-security-scan.sh'; then
  report "hard-coded secret-like string detected"
fi

public_ips="$(
  git grep -n -I -E '([0-9]{1,3}\.){3}[0-9]{1,3}' -- . \
    ':!scripts/ci-security-scan.sh' \
    ':!package-lock.json' \
    | grep -vE '127\.0\.0\.1|0\.0\.0\.0' || true
)"
if [[ -n "${public_ips}" ]]; then
  printf '%s\n' "${public_ips}"
  report "hard-coded non-loopback IP address detected"
fi

if (( failures > 0 )); then
  echo "security scan failed with ${failures} issue(s)" >&2
  exit 1
fi

echo "security scan passed"
