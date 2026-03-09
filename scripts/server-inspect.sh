#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${ROOT_DIR}/.env.prod"
OUTPUT_DIR="${ROOT_DIR}/artifacts/ops"
mkdir -p "${OUTPUT_DIR}"

if [[ -f "${ENV_FILE}" ]]; then
  # shellcheck disable=SC1090
  source "${ENV_FILE}"
fi

timestamp="$(date +%Y%m%d%H%M%S)"
report_file="${OUTPUT_DIR}/server_inspect_${timestamp}.log"

{
  echo "generated_at=$(date -Is)"
  echo "hostname=$(hostname)"
  echo "kernel=$(uname -sr)"
  echo
  echo "[disk]"
  df -h
  echo
  echo "[memory]"
  free -h || true
  echo
  echo "[load]"
  uptime
  echo
  echo "[gateway_health]"
  if [[ -n "${APP_DOMAIN:-}" ]]; then
    curl --silent --show-error --max-time 10 "https://${APP_DOMAIN}/actuator/health" || true
    echo
  else
    curl --silent --show-error --max-time 10 "http://localhost/actuator/health" || true
    echo
  fi
  echo
  echo "[certificate_expiry]"
  if [[ -n "${APP_DOMAIN:-}" ]]; then
    cert_path="${ROOT_DIR}/ops/certbot/conf/live/${APP_DOMAIN}/fullchain.pem"
    if [[ -f "${cert_path}" ]]; then
      openssl x509 -in "${cert_path}" -noout -enddate
    else
      echo "certificate not found: ${cert_path}"
    fi
  else
    echo "APP_DOMAIN not configured"
  fi
} | tee "${report_file}"

echo "Inspection report: ${report_file}"
