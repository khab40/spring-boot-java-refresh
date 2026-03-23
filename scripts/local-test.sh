#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/local-common.sh"

ensure_command mvn
ensure_java21_runtime
ensure_frontend_dependencies

(
  cd "${REPO_ROOT}"
  mvn test "$@"
)

(
  cd "${REPO_ROOT}/frontend"
  npm test
)
