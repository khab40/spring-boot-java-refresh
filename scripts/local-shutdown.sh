#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/local-common.sh"

stop_process "${BACKEND_PID_FILE}" "Local backend"
stop_process "${FRONTEND_PID_FILE}" "Local frontend"
stop_stripe_listener

if [[ "${MDL_LOCAL_SUPPORT_SHUTDOWN:-false}" == "true" ]]; then
  run_compose stop ${MDL_LOCAL_SUPPORT_SERVICES:-fss mailpit} || true
fi
