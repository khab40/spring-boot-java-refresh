#!/usr/bin/env bash

set -euo pipefail

if [[ -n "${BASH_SOURCE[0]:-}" ]]; then
  SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
elif [[ -n "${ZSH_VERSION:-}" ]]; then
  SCRIPT_DIR="$(cd "$(dirname "$(eval 'printf %s "${(%):-%x}"')")" && pwd)"
else
  SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
fi
source "${SCRIPT_DIR}/common.sh"

LOCAL_RUNTIME_DIR="${MDL_RUNTIME_DIR}/local"
LOCAL_LOG_DIR="${LOCAL_RUNTIME_DIR}/logs"
BACKEND_PID_FILE="${LOCAL_RUNTIME_DIR}/backend.pid"
FRONTEND_PID_FILE="${LOCAL_RUNTIME_DIR}/frontend.pid"
BACKEND_LOG_FILE="${LOCAL_LOG_DIR}/backend.log"
FRONTEND_LOG_FILE="${LOCAL_LOG_DIR}/frontend.log"

mkdir -p "${LOCAL_LOG_DIR}"

ensure_command() {
  local command_name="$1"
  if ! command -v "${command_name}" >/dev/null 2>&1; then
    echo "Required command not found: ${command_name}" >&2
    exit 1
  fi
}

ensure_node_runtime() {
  if [[ -s "${HOME}/.nvm/nvm.sh" ]]; then
    # shellcheck disable=SC1091
    source "${HOME}/.nvm/nvm.sh"
    nvm use >/dev/null
  fi

  ensure_command node
  ensure_command npm
}

setup_java21_env() {
  local preferred_java_home

  preferred_java_home="${JAVA_HOME:-}"
  if [[ -z "${preferred_java_home}" ]] || [[ ! -x "${preferred_java_home}/bin/java" ]]; then
    preferred_java_home="/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home"
  fi

  if [[ ! -x "${preferred_java_home}/bin/java" ]]; then
    preferred_java_home="$(/usr/libexec/java_home -v 21 2>/dev/null || true)"
  fi

  if [[ -n "${preferred_java_home}" ]] && [[ -x "${preferred_java_home}/bin/java" ]]; then
    export JAVA_HOME="${preferred_java_home}"
    export PATH="${JAVA_HOME}/bin:${PATH}"
  fi
}

ensure_java21_runtime() {
  ensure_command java
  ensure_command mvn

  setup_java21_env

  local java_version
  java_version="$(java -version 2>&1 | awk -F '"' '/version/ { print $2; exit }')"

  if [[ -z "${java_version}" ]]; then
    echo "Unable to determine local Java runtime version." >&2
    exit 1
  fi

  if [[ "${java_version}" != 21* ]]; then
    echo "Local backend runtime requires Java 21. Current java version is ${java_version}." >&2
    echo "Install/select Java 21 locally before using host-mode backend scripts." >&2
    exit 1
  fi
}

process_running() {
  local pid_file="$1"
  [[ -f "${pid_file}" ]] || return 1

  local pid
  pid="$(cat "${pid_file}")"
  [[ -n "${pid}" ]] && kill -0 "${pid}" 2>/dev/null
}

stop_process() {
  local pid_file="$1"
  local label="$2"

  if ! process_running "${pid_file}"; then
    rm -f "${pid_file}"
    return 0
  fi

  local pid
  pid="$(cat "${pid_file}")"
  kill "${pid}" 2>/dev/null || true
  rm -f "${pid_file}"
  echo "${label} stopped."
}

ensure_frontend_dependencies() {
  ensure_node_runtime
  if [[ ! -d "${REPO_ROOT}/frontend/node_modules" ]]; then
    (
      cd "${REPO_ROOT}/frontend"
      npm install
    )
  fi
}

start_background_process() {
  local pid_file="$1"
  local log_file="$2"
  shift 2

  if process_running "${pid_file}"; then
    echo "Process already running for ${pid_file} (pid $(cat "${pid_file}"))."
    return 0
  fi

  (
    cd "${REPO_ROOT}"
    nohup "$@" > "${log_file}" 2>&1 &
    echo $! > "${pid_file}"
  )
}

start_local_support_services() {
  local services
  services="${MDL_LOCAL_SUPPORT_SERVICES:-fss mailpit}"

  if [[ "${MDL_LOCAL_SUPPORT:-true}" != "true" ]] || [[ -z "${services}" ]]; then
    return 0
  fi

  if command -v docker >/dev/null 2>&1 || command -v docker-compose >/dev/null 2>&1; then
    run_compose up -d ${services}
  else
    echo "Docker not found. Skipping optional support services (${services})." >&2
  fi
}
