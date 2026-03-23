#!/usr/bin/env bash

set -euo pipefail

if [[ -n "${BASH_SOURCE[0]:-}" ]]; then
  SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
elif [[ -n "${ZSH_VERSION:-}" ]]; then
  SCRIPT_DIR="$(cd "$(dirname "$(eval 'printf %s "${(%):-%x}"')")" && pwd)"
else
  SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
fi
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
MDL_RUNTIME_DIR="${TMPDIR:-/tmp}/market-data-lake"
STRIPE_LISTENER_PID_FILE="${MDL_RUNTIME_DIR}/stripe-listen.pid"
STRIPE_LISTENER_LOG_FILE="${MDL_RUNTIME_DIR}/stripe-listen.log"

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

mkdir -p "${MDL_RUNTIME_DIR}"

DOCKER_COMPOSE_CMD=()

init_docker_compose() {
  if [[ ${#DOCKER_COMPOSE_CMD[@]} -gt 0 ]]; then
    return
  fi

  if command -v docker >/dev/null 2>&1 && docker compose version >/dev/null 2>&1; then
    DOCKER_COMPOSE_CMD=(docker compose)
  elif command -v docker-compose >/dev/null 2>&1; then
    DOCKER_COMPOSE_CMD=(docker-compose)
  else
    echo "Docker Compose is not installed." >&2
    exit 1
  fi
}

run_compose() {
  init_docker_compose
  (
    cd "${REPO_ROOT}"
    "${DOCKER_COMPOSE_CMD[@]}" "$@"
  )
}

stripe_listener_running() {
  if [[ ! -f "${STRIPE_LISTENER_PID_FILE}" ]]; then
    return 1
  fi

  local pid
  pid="$(cat "${STRIPE_LISTENER_PID_FILE}")"
  [[ -n "${pid}" ]] && kill -0 "${pid}" 2>/dev/null
}

start_stripe_listener() {
  local auto_listen
  auto_listen="${MDL_AUTO_STRIPE_LISTEN:-true}"

  if [[ "${auto_listen}" != "true" ]]; then
    return 0
  fi

  if ! command -v stripe >/dev/null 2>&1; then
    echo "Stripe CLI not found. Skipping automatic webhook listener startup." >&2
    return 0
  fi

  if stripe_listener_running; then
    echo "Stripe webhook listener already running (pid $(cat "${STRIPE_LISTENER_PID_FILE}"))."
    return 0
  fi

  (
    cd "${REPO_ROOT}"
    nohup "${SCRIPT_DIR}/stripe-listen.sh" > "${STRIPE_LISTENER_LOG_FILE}" 2>&1 &
    echo $! > "${STRIPE_LISTENER_PID_FILE}"
  )

  echo "Stripe webhook listener started (pid $(cat "${STRIPE_LISTENER_PID_FILE}"))."
  echo "Stripe listener log: ${STRIPE_LISTENER_LOG_FILE}"
}

stop_stripe_listener() {
  if ! stripe_listener_running; then
    rm -f "${STRIPE_LISTENER_PID_FILE}"
    return 0
  fi

  local pid
  pid="$(cat "${STRIPE_LISTENER_PID_FILE}")"
  kill "${pid}" 2>/dev/null || true
  rm -f "${STRIPE_LISTENER_PID_FILE}"
  echo "Stripe webhook listener stopped."
}
