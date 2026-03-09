#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

mkdir -p "${ROOT_DIR}/ops/nginx/generated"
mkdir -p "${ROOT_DIR}/ops/certbot/www"
mkdir -p "${ROOT_DIR}/ops/certbot/conf"
mkdir -p "${ROOT_DIR}/artifacts/backup"
mkdir -p "${ROOT_DIR}/artifacts/perf"

if [[ ! -f "${ROOT_DIR}/.env.prod" ]]; then
  cp "${ROOT_DIR}/.env.prod.example" "${ROOT_DIR}/.env.prod"
  echo "Created ${ROOT_DIR}/.env.prod from template."
else
  echo ".env.prod already exists."
fi

echo "Bootstrap completed."
