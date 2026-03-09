#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SYSTEMD_DIR="${ROOT_DIR}/ops/systemd"
TARGET_DIR="/etc/systemd/system"

if [[ $EUID -ne 0 ]]; then
  echo "Please run as root." >&2
  exit 1
fi

if [[ ! -d "${SYSTEMD_DIR}" ]]; then
  echo "Systemd template directory not found: ${SYSTEMD_DIR}" >&2
  exit 1
fi

cp "${SYSTEMD_DIR}/homework-prod.service" "${TARGET_DIR}/homework-prod.service"
cp "${SYSTEMD_DIR}/homework-cert-renew.service" "${TARGET_DIR}/homework-cert-renew.service"
cp "${SYSTEMD_DIR}/homework-cert-renew.timer" "${TARGET_DIR}/homework-cert-renew.timer"
cp "${SYSTEMD_DIR}/homework-inspect.service" "${TARGET_DIR}/homework-inspect.service"
cp "${SYSTEMD_DIR}/homework-inspect.timer" "${TARGET_DIR}/homework-inspect.timer"

systemctl daemon-reload
systemctl enable homework-prod.service
systemctl enable homework-cert-renew.timer
systemctl enable homework-inspect.timer

echo "Installed systemd units."
echo "Start app stack: systemctl start homework-prod.service"
echo "Start renew timer: systemctl start homework-cert-renew.timer"
echo "Start inspect timer: systemctl start homework-inspect.timer"
