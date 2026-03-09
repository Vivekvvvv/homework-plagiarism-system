#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SOURCE_FILE="${ROOT_DIR}/ops/logrotate/homework-ops.conf"
TARGET_FILE="/etc/logrotate.d/homework-ops"

if [[ $EUID -ne 0 ]]; then
  echo "Please run as root." >&2
  exit 1
fi

if [[ ! -f "${SOURCE_FILE}" ]]; then
  echo "Logrotate template not found: ${SOURCE_FILE}" >&2
  exit 1
fi

cp "${SOURCE_FILE}" "${TARGET_FILE}"
chmod 644 "${TARGET_FILE}"

echo "Installed logrotate config at ${TARGET_FILE}."
