#!/bin/bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-}"
find_mysql_client() {
  if [[ -n "${MYSQL_BIN:-}" && -x "$MYSQL_BIN/mysql.exe" ]]; then
    echo "$MYSQL_BIN/mysql.exe"
    return 0
  fi

  local candidates=(
    "/c/Program Files/MySQL/MySQL Server 8.0/bin/mysql.exe"
    "/c/Program Files/MySQL/MySQL Server 8.4/bin/mysql.exe"
    "/c/Program Files/MariaDB 11.*/bin/mysql.exe"
  )

  local candidate
  for candidate in "${candidates[@]}"; do
    if compgen -G "$candidate" > /dev/null; then
      echo $(compgen -G "$candidate" | head -n 1)
      return 0
    fi
  done

  if command -v mysql >/dev/null 2>&1; then
    command -v mysql
    return 0
  fi

  return 1
}

verify_mysql_connection() {
  if ! "$MYSQL_CLIENT" --user="$MYSQL_USER" --password="$MYSQL_PASSWORD" --connect-timeout=5 -e "SELECT 1" >/dev/null 2>&1; then
    echo "Cannot connect to MySQL. Ensure the server is running and credentials are correct."
    exit 1
  fi
  echo "MySQL connection OK."
}

run_sql_file() {
  local mysql_client="$1"
  local sql_file="$2"

  echo "Running $(basename "$sql_file")..."
  "$mysql_client" --user="$MYSQL_USER" --password="$MYSQL_PASSWORD" < "$sql_file"
}

if [[ -z "$MYSQL_PASSWORD" ]]; then
  echo -n "Enter MySQL password for user '$MYSQL_USER': " >/dev/tty
  read -r -s MYSQL_PASSWORD </dev/tty
  echo >/dev/tty
fi

MYSQL_CLIENT="$(find_mysql_client)" || {
  echo "mysql client not found. Set MYSQL_BIN to your MySQL bin directory or add mysql to PATH."
  exit 1
}

echo "Using MySQL client: $MYSQL_CLIENT"
verify_mysql_connection

run_sql_file "$MYSQL_CLIENT" "$REPO_ROOT/create_database.sql"
run_sql_file "$MYSQL_CLIENT" "$REPO_ROOT/ddl_schema.sql"
run_sql_file "$MYSQL_CLIENT" "$REPO_ROOT/01_seed_data.sql"
run_sql_file "$MYSQL_CLIENT" "$REPO_ROOT/views_stored_procedures.sql"

echo "Database bootstrap completed successfully."