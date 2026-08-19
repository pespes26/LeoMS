#!/bin/sh
set -eu

repo_dir=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
secret_dir="$repo_dir/secrets"
mkdir -p "$secret_dir"
umask 077

create_random() {
  target="$secret_dir/$1"
  if [ -e "$target" ]; then
    printf 'Keeping existing %s\n' "$target"
  else
    openssl rand -hex 32 > "$target"
    printf 'Created %s\n' "$target"
  fi
}

create_random mysql_root_password
create_random mysql_game_password
create_random mysql_admin_password
create_random mysql_backup_password
create_random restic_password

if [ ! -e "$secret_dir/admin_password_hash" ]; then
  printf 'Admin password (16+ characters): ' >&2
  stty -echo
  IFS= read -r admin_password
  stty echo
  printf '\n' >&2
  if [ "${#admin_password}" -lt 16 ]; then
    echo 'Password is too short.' >&2
    exit 1
  fi
  printf '%s\n' "$admin_password" | docker run --rm -i httpd:2.4-alpine sh -c \
    'IFS= read -r p; htpasswd -bnBC 12 "" "$p" | tr -d ":\n"' > "$secret_dir/admin_password_hash"
  printf '\n' >> "$secret_dir/admin_password_hash"
  unset admin_password
  printf 'Created %s\n' "$secret_dir/admin_password_hash"
fi

for key in s3_access_key s3_secret_key; do
  target="$secret_dir/$key"
  if [ ! -e "$target" ]; then
    printf '%s: ' "$key" >&2
    stty -echo
    IFS= read -r value
    stty echo
    printf '\n' >&2
    [ -n "$value" ] || { echo "$key cannot be empty" >&2; exit 1; }
    printf '%s\n' "$value" > "$target"
    unset value
  fi
done

chmod 600 "$secret_dir"/*
echo 'Secrets are ready. Copy .env.example to .env and set non-secret deployment values.'
