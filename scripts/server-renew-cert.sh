#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${ROOT_DIR}/.env.prod"

if [[ ! -f "${ENV_FILE}" ]]; then
  echo ".env.prod not found. Please run scripts/server-bootstrap.sh first." >&2
  exit 1
fi

set -a
# shellcheck disable=SC1090
source "${ENV_FILE}"
set +a

if ! command -v docker >/dev/null 2>&1; then
  echo "docker not found. Please install Docker first." >&2
  exit 1
fi

mkdir -p "${ROOT_DIR}/ops/certbot/www"
mkdir -p "${ROOT_DIR}/ops/certbot/conf"

docker run --rm \
  -v "${ROOT_DIR}/ops/certbot/conf:/etc/letsencrypt" \
  -v "${ROOT_DIR}/ops/certbot/www:/var/www/certbot" \
  certbot/certbot renew --webroot -w /var/www/certbot

if docker ps --format '{{.Names}}' | grep -q "^homework-nginx$"; then
  docker exec homework-nginx nginx -s reload >/dev/null
fi

echo "Certificate renewal finished."
