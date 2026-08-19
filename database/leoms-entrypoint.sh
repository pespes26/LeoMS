#!/bin/bash
set -Eeuo pipefail

read_nonempty_secret() {
  local path="$1"
  local value
  value="$(tr -d '\r\n' < "$path")"
  [[ -n "$value" ]] || { echo "Empty secret: $path" >&2; return 1; }
  printf '%s' "$value"
}

validate_account_secret() {
  local path="$1"
  local value
  value="$(tr -d '\r\n' < "$path")"
  if [[ ! "$value" =~ ^[A-Za-z0-9_.-]{24,128}$ ]]; then
    echo "Secret at $path must be 24-128 characters from A-Z, a-z, 0-9, _, ., or -." >&2
    return 1
  fi
}

# Resolve the root file secret inside the container without adding it to the
# Compose environment. The watcher needs it to apply grants after migrations.
if [[ -n "${MYSQL_ROOT_PASSWORD_FILE:-}" ]]; then
  export MYSQL_ROOT_PASSWORD
  MYSQL_ROOT_PASSWORD="$(read_nonempty_secret "$MYSQL_ROOT_PASSWORD_FILE")"
  unset MYSQL_ROOT_PASSWORD_FILE
fi

# Fail the container before MySQL starts when a service-account secret is
# absent or malformed; otherwise the database could appear healthy without
# the restricted users being provisioned.
validate_account_secret "${LEOMS_ADMIN_PASSWORD_FILE:?missing LEOMS_ADMIN_PASSWORD_FILE}"
validate_account_secret "${LEOMS_BACKUP_PASSWORD_FILE:?missing LEOMS_BACKUP_PASSWORD_FILE}"

/usr/local/bin/leoms-grant-watcher.sh &
exec /usr/local/bin/docker-entrypoint.sh "$@"
