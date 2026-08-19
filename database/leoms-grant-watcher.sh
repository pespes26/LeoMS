#!/bin/bash
set -Eeuo pipefail

read_account_secret() {
  local path="$1"
  local value
  value="$(tr -d '\r\n' < "$path")"
  if [[ ! "$value" =~ ^[A-Za-z0-9_.-]{24,128}$ ]]; then
    echo "Secret at $path must be 24-128 characters from A-Z, a-z, 0-9, _, ., or -." >&2
    return 1
  fi
  printf '%s' "$value"
}

admin_password="$(read_account_secret "${LEOMS_ADMIN_PASSWORD_FILE:?missing LEOMS_ADMIN_PASSWORD_FILE}")"
backup_password="$(read_account_secret "${LEOMS_BACKUP_PASSWORD_FILE:?missing LEOMS_BACKUP_PASSWORD_FILE}")"
export MYSQL_PWD="${MYSQL_ROOT_PASSWORD:?missing MYSQL_ROOT_PASSWORD}"

required_tables=4
while true; do
  table_count="$(mysql --protocol=socket -uroot --batch --skip-column-names \
    -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='cosmic' AND table_name IN ('accounts','characters','leoms_admin_audit','leoms_backup_jobs');" \
    2>/dev/null || true)"
  [[ "$table_count" == "$required_tables" ]] && break
  sleep 5
done

mysql --protocol=socket -uroot <<SQL
CREATE USER IF NOT EXISTS 'leoms_admin'@'%' IDENTIFIED BY '${admin_password}';
ALTER USER 'leoms_admin'@'%' IDENTIFIED BY '${admin_password}';
GRANT SELECT ON cosmic.accounts TO 'leoms_admin'@'%';
GRANT INSERT (name, password, pin, pic, tos) ON cosmic.accounts TO 'leoms_admin'@'%';
GRANT UPDATE (password, pin, pic, banned, banreason) ON cosmic.accounts TO 'leoms_admin'@'%';
GRANT SELECT ON cosmic.characters TO 'leoms_admin'@'%';
GRANT SELECT, INSERT ON cosmic.leoms_admin_audit TO 'leoms_admin'@'%';
GRANT SELECT, INSERT ON cosmic.leoms_backup_jobs TO 'leoms_admin'@'%';

CREATE USER IF NOT EXISTS 'leoms_backup'@'%' IDENTIFIED BY '${backup_password}';
ALTER USER 'leoms_backup'@'%' IDENTIFIED BY '${backup_password}';
GRANT SELECT, SHOW VIEW, TRIGGER, EVENT, LOCK TABLES ON cosmic.* TO 'leoms_backup'@'%';
GRANT INSERT, UPDATE ON cosmic.leoms_backup_jobs TO 'leoms_backup'@'%';
FLUSH PRIVILEGES;
SQL

echo 'LeoMS restricted database users and grants are ready.'
