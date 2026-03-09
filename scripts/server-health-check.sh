#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${1:-http://localhost}"

check() {
  local name="$1"
  local path="$2"
  echo "Checking ${name}: ${BASE_URL}${path}"
  curl --fail --silent --show-error "${BASE_URL}${path}" > /dev/null
}

check "home" "/"
check "health" "/actuator/health"
check "info" "/actuator/info"
check "swagger" "/swagger-ui/index.html"

echo "All health checks passed."
