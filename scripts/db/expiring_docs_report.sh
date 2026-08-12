#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/_lib.sh"

REPORT_FILE="$SCRIPT_DIR/expiring_docs_report.csv"

prompt_password

MYSQL_CLIENT="$(find_mysql_client)" || {
  echo "mysql client not found. Set MYSQL_BIN to your MySQL bin directory or add mysql to PATH."
  exit 1
}

echo "Generating expiring documents report..."
"$MYSQL_CLIENT" --user="$MYSQL_USER" --password="$MYSQL_PASSWORD" kyc_db -B \
  -e "SELECT * FROM expiring_documents_vw;" | sed 's/\t/,/g' > "$REPORT_FILE"
echo "Report saved to $REPORT_FILE"