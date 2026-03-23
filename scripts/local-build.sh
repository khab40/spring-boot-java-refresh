#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/local-common.sh"

ensure_command mvn
ensure_java21_runtime
ensure_frontend_dependencies

(
  cd "${REPO_ROOT}"
  mvn -DskipTests package "$@"
)

(
  cd "${REPO_ROOT}/frontend"
  INTERNAL_API_BASE_URL="${INTERNAL_API_BASE_URL:-http://localhost:8080}" \
  NEXT_PUBLIC_AUTH_BASE_URL="${NEXT_PUBLIC_AUTH_BASE_URL:-http://localhost:8080}" \
  npm run build
)
