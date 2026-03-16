#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/common.sh"

docker run --rm \
  -e DOCKER_CONFIG="${DOCKER_CONFIG:-}" \
  -v "${HOME}/.m2:/root/.m2" \
  -v "${REPO_ROOT}:/workspace" \
  -w /workspace \
  maven:3.9.9-eclipse-temurin-21 \
  mvn -Dproject.build.directory=/tmp/market-data-lake-target clean test "$@"

docker run --rm \
  -v "${HOME}/.npm:/root/.npm" \
  -v "${REPO_ROOT}/frontend:/workspace" \
  -w /workspace \
  node:22-alpine \
  sh -lc "npm ci && npm test"
