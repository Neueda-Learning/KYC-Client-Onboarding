#!/bin/bash
# Shared helpers sourced by all db scripts. Set MYSQL_BIN to override client discovery.

MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-}"

_find_bin() {
  local name="$1"
  if [[ -n "${MYSQL_BIN:-}" && -x "$MYSQL_BIN/${name}.exe" ]]; then
    echo "$MYSQL_BIN/${name}.exe"
    return 0
  fi
  local candidates=(
    "/c/Program Files/MySQL/MySQL Server 8.0/bin/${name}.exe"
    "/c/Program Files/MySQL/MySQL Server 8.4/bin/${name}.exe"
    "/c/Program Files/MariaDB 11.*/bin/${name}.exe"
  )
  local candidate
  for candidate in "${candidates[@]}"; do
    if compgen -G "$candidate" > /dev/null; then
      echo "$(compgen -G "$candidate" | head -n 1)"
      return 0
    fi
  done
  if command -v "$name" >/dev/null 2>&1; then
    command -v "$name"
    return 0
  fi
  return 1
}

find_mysql_client() { _find_bin mysql; }
find_mysqldump()    { _find_bin mysqldump; }

prompt_password() {
  if [[ -z "$MYSQL_PASSWORD" ]]; then
    echo -n "Enter MySQL password for user '$MYSQL_USER': " >/dev/tty
    read -r -s MYSQL_PASSWORD </dev/tty
    echo >/dev/tty
  fi
}

verify_connection() {
  if ! "$MYSQL_CLIENT" --user="$MYSQL_USER" --password="$MYSQL_PASSWORD" --connect-timeout=5 -e "SELECT 1" >/dev/null 2>&1; then
    echo "Cannot connect to MySQL. Ensure the server is running and credentials are correct."
    exit 1
  fi
}
