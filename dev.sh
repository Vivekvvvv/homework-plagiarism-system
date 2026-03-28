#!/usr/bin/env bash
# 开发环境一键启动脚本
# 用法: bash dev.sh [frontend|backend|all]
# 默认: all

set -e

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
BACKEND_DIR="$ROOT_DIR/backend"
FRONTEND_DIR="$ROOT_DIR/frontend"

start_backend() {
  echo "[backend] 编译并打包..."
  cd "$BACKEND_DIR"
  mvn package -DskipTests -q
  echo "[backend] 启动 JAR (port 8081)..."
  java -jar target/homework-backend-0.1.0.jar &
  BACKEND_PID=$!
  echo "[backend] PID: $BACKEND_PID"
}

start_frontend() {
  echo "[frontend] 安装依赖并启动 (port 5173)..."
  cd "$FRONTEND_DIR"
  npm install --silent
  npm run dev &
  FRONTEND_PID=$!
  echo "[frontend] PID: $FRONTEND_PID"
}

MODE="${1:-all}"

case $MODE in
  backend)
    start_backend
    wait
    ;;
  frontend)
    start_frontend
    wait
    ;;
  all|*)
    start_backend
    start_frontend
    echo ""
    echo "============================="
    echo " 前端: http://localhost:5173"
    echo " 后端: http://localhost:8081"
    echo "============================="
    wait
    ;;
esac
