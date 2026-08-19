#!/bin/sh
set -eu

profile=${1:-local}
case "$profile" in local|vps) ;; *) echo 'Usage: monthly-restore-test.sh [local|vps]' >&2; exit 64;; esac

stamp=$(date -u +%Y%m%dT%H%M%SZ)
container="leoms-restore-test-$stamp"
volume="leoms-restore-test-$stamp"
restore_password=$(openssl rand -hex 24)

cleanup() {
  docker rm -f "$container" >/dev/null 2>&1 || true
  docker volume rm "$volume" >/dev/null 2>&1 || true
  unset restore_password
}
trap cleanup EXIT HUP INT TERM

echo 'Restoring the selected encrypted restic snapshot into the backup cache...'
docker compose --profile "$profile" run --rm backup restore-latest

docker volume create "$volume" >/dev/null
docker run -d --name "$container" --mount "source=$volume,target=/var/lib/mysql" \
  -e MYSQL_DATABASE=cosmic -e MYSQL_ROOT_PASSWORD="$restore_password" mysql:8.4.0 >/dev/null

tries=0
until docker exec "$container" mysqladmin ping -uroot -p"$restore_password" --silent; do
  tries=$((tries + 1))
  [ "$tries" -lt 60 ] || { echo 'Disposable MySQL did not become healthy.' >&2; exit 1; }
  sleep 2
done

docker compose --profile "$profile" run --rm --entrypoint sh backup -c \
  'dump=$(find /var/lib/leoms-backup/restore -type f -name "leoms-*.sql.gz" | sort | tail -1); [ -n "$dump" ]; gzip -dc "$dump"' \
  | docker exec -i "$container" mysql -uroot -p"$restore_password" cosmic

tables=$(docker exec "$container" mysql -uroot -p"$restore_password" --batch --skip-column-names cosmic -e \
  "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='cosmic' AND table_name IN ('accounts','characters','leoms_admin_audit','leoms_backup_jobs');")
[ "$tables" -eq 4 ] || { echo "Restore validation failed: found $tables of 4 critical tables." >&2; exit 1; }

counts=$(docker exec "$container" mysql -uroot -p"$restore_password" --batch --skip-column-names cosmic -e \
  "SELECT CONCAT('accounts=', COUNT(*)) FROM accounts UNION ALL SELECT CONCAT('characters=', COUNT(*)) FROM characters UNION ALL SELECT CONCAT('audit=', COUNT(*)) FROM leoms_admin_audit;")
printf 'Monthly restore test passed (%s).\n%s\n' "$stamp" "$counts"
