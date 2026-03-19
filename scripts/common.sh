#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

load_env_file() {
  local env_file
  env_file="${REPO_ROOT}/.env"

  if [[ -f "${env_file}" ]]; then
    set -a
    # shellcheck disable=SC1090
    source "${env_file}"
    set +a
  fi
}

prepare_docker_env() {
  local docker_config_source docker_helper

  docker_config_source="${DOCKER_CONFIG:-${HOME}/.docker}"
  docker_helper="$(awk -F'"' '/"credsStore"[[:space:]]*:/ { print $4; exit }' "${docker_config_source}/config.json" 2>/dev/null || true)"

  if [[ -z "${docker_helper}" ]] || command -v "docker-credential-${docker_helper}" >/dev/null 2>&1; then
    return
  fi

  export DOCKER_CONFIG
  DOCKER_CONFIG="$(mktemp -d)"
  trap 'rm -rf "${DOCKER_CONFIG}"' EXIT

  if [[ -f "${docker_config_source}/config.json" ]]; then
    cp -R "${docker_config_source}/." "${DOCKER_CONFIG}/"
    sed '/"credsStore"[[:space:]]*:/d' "${docker_config_source}/config.json" > "${DOCKER_CONFIG}/config.json"
  else
    printf '{ "auths": {} }\n' > "${DOCKER_CONFIG}/config.json"
  fi
}

prepare_docker_host() {
  local docker_config_source context_name docker_host

  if [[ -n "${DOCKER_HOST:-}" ]]; then
    return
  fi

  docker_config_source="${DOCKER_CONFIG:-${HOME}/.docker}"
  context_name="$(awk -F'"' '/"currentContext"[[:space:]]*:/ { print $4; exit }' "${docker_config_source}/config.json" 2>/dev/null || true)"
  context_name="${context_name:-default}"

  docker_host="$(docker context inspect "${context_name}" 2>/dev/null | awk -F'"' '/"Host"[[:space:]]*:/ { print $4; exit }' || true)"
  if [[ -n "${docker_host}" ]]; then
    export DOCKER_HOST="${docker_host}"
  fi
}

load_env_file
prepare_docker_env
prepare_docker_host

if docker compose version >/dev/null 2>&1; then
  DOCKER_COMPOSE_CMD=(docker compose)
elif command -v docker-compose >/dev/null 2>&1; then
  DOCKER_COMPOSE_CMD=(docker-compose)
else
  echo "Docker Compose is not installed." >&2
  exit 1
fi

run_compose() {
  (
    cd "${REPO_ROOT}"
    "${DOCKER_COMPOSE_CMD[@]}" "$@"
  )
}
