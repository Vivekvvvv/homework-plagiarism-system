#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${ROOT_DIR}/.env.prod"
NGINX_TEMPLATE="${ROOT_DIR}/ops/nginx/prod.http.conf.template"
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

if [[ ! -f "${NGINX_TEMPLATE}" ]]; then
  echo "Nginx template not found: ${NGINX_TEMPLATE}" >&2
  exit 1
fi

if ! command -v docker >/dev/null 2>&1; then
  echo "docker not found. Please install Docker first." >&2
  exit 1
fi

mkdir -p "${ROOT_DIR}/ops/nginx/generated"
mkdir -p "${ROOT_DIR}/artifacts/uploads"
mkdir -p "${ROOT_DIR}/ops/certbot/www"
mkdir -p "${ROOT_DIR}/ops/certbot/conf"

sed "s/__APP_DOMAIN__/${APP_DOMAIN}/g" "${NGINX_TEMPLATE}" > "${NGINX_OUTPUT}"

NETWORK_NAME="homework-net"
MYSQL_CONTAINER="homework-mysql"
BACKEND_CONTAINER="homework-backend"
FRONTEND_CONTAINER="homework-frontend"
NGINX_CONTAINER="homework-nginx"

if ! docker network inspect "${NETWORK_NAME}" >/dev/null 2>&1; then
  docker network create "${NETWORK_NAME}" >/dev/null
fi

if ! docker ps -a --format '{{.Names}}' | grep -q "^${MYSQL_CONTAINER}$"; then
  docker run -d --name "${MYSQL_CONTAINER}" \
    --network "${NETWORK_NAME}" \
    -e MYSQL_ROOT_PASSWORD="${MYSQL_ROOT_PASSWORD:-root}" \
    -e MYSQL_DATABASE="${MYSQL_DATABASE:-homework_plagiarism}" \
    -v "${ROOT_DIR}/backend/src/main/resources/sql/init.sql:/docker-entrypoint-initdb.d/init.sql:ro" \
    -v homework-mysql-data:/var/lib/mysql \
    mysql:8.0 \
    --character-set-server=utf8mb4 \
    --collation-server=utf8mb4_0900_ai_ci >/dev/null
else
  docker start "${MYSQL_CONTAINER}" >/dev/null
fi

echo "Waiting for MySQL to be ready..."
mysql_ready="false"
for _ in $(seq 1 30); do
  if docker exec "${MYSQL_CONTAINER}" mysqladmin ping -uroot -p"${MYSQL_ROOT_PASSWORD:-root}" --silent >/dev/null 2>&1; then
    mysql_ready="true"
    break
  fi
  sleep 2
done
if [[ "${mysql_ready}" != "true" ]]; then
  echo "MySQL did not become ready in time." >&2
  exit 1
fi

docker build -t homework-backend "${ROOT_DIR}/backend" >/dev/null
docker build -t homework-frontend \
  --build-arg VITE_API_BASE_URL="${VITE_API_BASE_URL:-/api/v1}" \
  "${ROOT_DIR}/frontend" >/dev/null

if docker ps -a --format '{{.Names}}' | grep -q "^${BACKEND_CONTAINER}$"; then
  docker rm -f "${BACKEND_CONTAINER}" >/dev/null
fi

UPLOAD_DIR_VALUE="${UPLOAD_DIR:-/app/uploads}"
docker run -d --name "${BACKEND_CONTAINER}" \
  --network "${NETWORK_NAME}" \
  -e DB_HOST="${DB_HOST:-mysql}" \
  -e DB_PORT="${DB_PORT:-3306}" \
  -e DB_NAME="${DB_NAME:-homework_plagiarism}" \
  -e DB_USER="${DB_USER:-root}" \
  -e DB_PASSWORD="${DB_PASSWORD:-root}" \
  -e JWT_SECRET="${JWT_SECRET:-}" \
  -e JWT_EXP_SECONDS="${JWT_EXP_SECONDS:-7200}" \
  -e UPLOAD_DIR="${UPLOAD_DIR_VALUE}" \
  -e SERVER_PORT=8080 \
  -v "${ROOT_DIR}/artifacts/uploads:${UPLOAD_DIR_VALUE}" \
  homework-backend >/dev/null

if docker ps -a --format '{{.Names}}' | grep -q "^${FRONTEND_CONTAINER}$"; then
  docker rm -f "${FRONTEND_CONTAINER}" >/dev/null
fi

docker run -d --name "${FRONTEND_CONTAINER}" \
  --network "${NETWORK_NAME}" \
  homework-frontend >/dev/null

if docker ps -a --format '{{.Names}}' | grep -q "^${NGINX_CONTAINER}$"; then
  docker rm -f "${NGINX_CONTAINER}" >/dev/null
fi

PUBLIC_HTTP_PORT_VALUE="${PUBLIC_HTTP_PORT:-80}"
docker run -d --name "${NGINX_CONTAINER}" \
  --network "${NETWORK_NAME}" \
  -p "${PUBLIC_HTTP_PORT_VALUE}:80" \
  -v "${NGINX_OUTPUT}:/etc/nginx/conf.d/default.conf:ro" \
  -v "${ROOT_DIR}/ops/certbot/www:/var/www/certbot" \
  -v "${ROOT_DIR}/ops/certbot/conf:/etc/letsencrypt" \
  nginx:1.27-alpine >/dev/null

echo "HTTP production stack deployed for ${APP_DOMAIN}."
