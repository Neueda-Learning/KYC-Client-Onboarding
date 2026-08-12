#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/_lib.sh"

BACKUP_FILE="$SCRIPT_DIR/kyc_db_backup.sql"

prompt_password

MYSQLDUMP="$(find_mysqldump)" || {
  echo "mysqldump not found. Set MYSQL_BIN to your MySQL bin directory or add mysqldump to PATH."
  exit 1
}

echo "Creating database backup for kyc_db..."
"$MYSQLDUMP" --user="$MYSQL_USER" --password="$MYSQL_PASSWORD" kyc_db > "$BACKUP_FILE"
echo "Backup saved to $BACKUP_FILE"