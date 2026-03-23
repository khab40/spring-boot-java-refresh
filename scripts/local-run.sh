#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/local-common.sh"

ensure_java21_runtime
ensure_frontend_dependencies
start_local_support_services

export APP_FRONTEND_ORIGIN="${APP_FRONTEND_ORIGIN:-http://localhost:3000}"
export APP_AUTH_OAUTH2_SUCCESS_URL="${APP_AUTH_OAUTH2_SUCCESS_URL:-http://localhost:3000/oauth/callback}"
export APP_AUTH_OAUTH2_FAILURE_URL="${APP_AUTH_OAUTH2_FAILURE_URL:-http://localhost:3000/?authError=google-signin-failed}"
export APP_AUTH_GOOGLE_REDIRECT_URI="${APP_AUTH_GOOGLE_REDIRECT_URI:-http://localhost:8080/login/oauth2/code/google}"
export APP_MONITORING_FRONTEND_HEALTH_URL="${APP_MONITORING_FRONTEND_HEALTH_URL:-http://localhost:3000/api/health}"
export APP_FSS_ENDPOINT="${APP_FSS_ENDPOINT:-http://localhost:9000}"
export APP_FSS_PUBLIC_ENDPOINT="${APP_FSS_PUBLIC_ENDPOINT:-http://localhost:9000}"

start_background_process "${BACKEND_PID_FILE}" "${BACKEND_LOG_FILE}" \
  mvn -f "${REPO_ROOT}/pom.xml" spring-boot:run

start_background_process "${FRONTEND_PID_FILE}" "${FRONTEND_LOG_FILE}" \
  /bin/zsh -lc "source \"${HOME}/.nvm/nvm.sh\" >/dev/null 2>&1 || true; cd \"${REPO_ROOT}/frontend\" && INTERNAL_API_BASE_URL=\"${INTERNAL_API_BASE_URL:-http://localhost:8080}\" NEXT_PUBLIC_API_BASE_URL=\"${NEXT_PUBLIC_API_BASE_URL:-/backend}\" NEXT_PUBLIC_AUTH_BASE_URL=\"${NEXT_PUBLIC_AUTH_BASE_URL:-http://localhost:8080}\" npm run dev -- ${FRONTEND_DEV_FLAGS:---turbopack} --hostname 127.0.0.1 --port 3000"

start_stripe_listener

echo "Local backend:  http://localhost:8080"
echo "Local frontend: http://localhost:3000"
echo "Backend log:    ${BACKEND_LOG_FILE}"
echo "Frontend log:   ${FRONTEND_LOG_FILE}"
