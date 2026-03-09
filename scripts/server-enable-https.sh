#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${ROOT_DIR}/.env.prod"
NGINX_TEMPLATE="${ROOT_DIR}/ops/nginx/prod.https.conf.template"
NGINX_OUTPUT="${ROOT_DIR}/ops/nginx/generated/default.conf"

if [[ ! -f "${ENV_FILE}" ]]; then
  echo ".env.prod not found. Please run scripts/server-bootstrap.sh first." >&2
  exit 1
fi

set -a
# shellcheck disable=SC1090
source "${ENV_FILE}"
set +a

if [[ -z "${APP_DOMAIN:-}" ]]; then
  echo "APP_DOMAIN is required in .env.prod" >&2
  exit 1
fi

if [[ -z "${CERTBOT_EMAIL:-}" ]]; then
  echo "CERTBOT_EMAIL is required in .env.prod" >&2
  exit 1
fi

if [[ ! -f "${NGINX_TEMPLATE}" ]]; then
  echo "Nginx template not found: ${NGINX_TEMPLATE}" >&2
  exit 1
fi

if ! command -v docker >/dev/null 2>&1; then
  echo "docker not found. Please install Docker first." >&2
  exit 1
fi

mkdir -p "${ROOT_DIR}/ops/nginx/generated"
mkdir -p "${ROOT_DIR}/ops/certbot/www"
mkdir -p "${ROOT_DIR}/ops/certbot/conf"

NGINX_CONTAINER="homework-nginx"

if ! docker ps -a --format '{{.Names}}' | grep -q "^${NGINX_CONTAINER}$"; then
  "${ROOT_DIR}/scripts/server-deploy-prod.sh"
fi

docker run --rm \
  -v "${ROOT_DIR}/ops/certbot/conf:/etc/letsencrypt" \
  -v "${ROOT_DIR}/ops/certbot/www:/var/www/certbot" \
  certbot/certbot certonly \
  --webroot -w /var/www/certbot \
  -d "${APP_DOMAIN}" \
  -m "${CERTBOT_EMAIL}" \
  --agree-tos \
  --no-eff-email

sed "s/__APP_DOMAIN__/${APP_DOMAIN}/g" "${NGINX_TEMPLATE}" > "${NGINX_OUTPUT}"

if docker ps -a --format '{{.Names}}' | grep -q "^${NGINX_CONTAINER}$"; then
  docker rm -f "${NGINX_CONTAINER}" >/dev/null
fi

PUBLIC_HTTP_PORT_VALUE="${PUBLIC_HTTP_PORT:-80}"
PUBLIC_HTTPS_PORT_VALUE="${PUBLIC_HTTPS_PORT:-443}"

docker run -d --name "${NGINX_CONTAINER}" \
  --network "homework-net" \
  -p "${PUBLIC_HTTP_PORT_VALUE}:80" \
  -p "${PUBLIC_HTTPS_PORT_VALUE}:443" \
  -v "${NGINX_OUTPUT}:/etc/nginx/conf.d/default.conf:ro" \
  -v "${ROOT_DIR}/ops/certbot/www:/var/www/certbot" \
  -v "${ROOT_DIR}/ops/certbot/conf:/etc/letsencrypt" \
  nginx:1.27-alpine >/dev/null

echo "HTTPS enabled for ${APP_DOMAIN}."
