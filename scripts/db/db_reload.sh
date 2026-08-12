#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/_lib.sh"

BACKUP_FILE="$SCRIPT_DIR/kyc_db_backup.sql"

if [[ ! -f "$BACKUP_FILE" ]]; then
  echo "Backup file not found: $BACKUP_FILE"
  exit 1
fi

prompt_password

MYSQL_CLIENT="$(find_mysql_client)" || {
  echo "mysql client not found. Set MYSQL_BIN to your MySQL bin directory or add mysql to PATH."
  exit 1
}

echo "Using MySQL client: $MYSQL_CLIENT"
verify_connection

echo "Dropping and recreating database..."
"$MYSQL_CLIENT" --user="$MYSQL_USER" --password="$MYSQL_PASSWORD" -e "DROP DATABASE IF EXISTS kyc_db; CREATE DATABASE kyc_db;"
"$MYSQL_CLIENT" --user="$MYSQL_USER" --password="$MYSQL_PASSWORD" kyc_db < "$BACKUP_FILE"
echo "Database successfully restored from backup."