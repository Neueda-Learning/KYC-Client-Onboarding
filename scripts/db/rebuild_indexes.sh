#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/_lib.sh"

prompt_password

MYSQL_CLIENT="$(find_mysql_client)" || {
  echo "mysql client not found. Set MYSQL_BIN to your MySQL bin directory or add mysql to PATH."
  exit 1
}

echo "Rebuilding and optimizing database indexes..."
"$MYSQL_CLIENT" --user="$MYSQL_USER" --password="$MYSQL_PASSWORD" kyc_db \
  -e "OPTIMIZE TABLE client, document, onboarding_case, client_address;"
echo "Indexes successfully rebuilt."