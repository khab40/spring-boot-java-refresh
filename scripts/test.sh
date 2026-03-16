#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/common.sh"

docker run --rm \
  -e DOCKER_CONFIG="${DOCKER_CONFIG:-}" \
  -v "${REPO_ROOT}:/workspace" \
  -w /workspace \
  maven:3.9.9-eclipse-temurin-21 \
  mvn test "$@"
