#!/bin/sh
set -eu

repo_dir=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$repo_dir"
failed=0

require() { rg -q "$1" "$2" || { echo "Missing required setting: $1 in $2" >&2; failed=1; }; }
reject() { if rg -q "$1" "$2"; then echo "Forbidden setting found: $1 in $2" >&2; failed=1; fi; }

require 'WORLDS: 1' config.yaml
require 'channels: 2' config.yaml
require 'exp_rate: 2' config.yaml
require 'meso_rate: 1' config.yaml
require 'drop_rate: 1' config.yaml
require 'boss_drop_rate: 1' config.yaml
require 'quest_rate: 1' config.yaml
require 'AUTOMATIC_REGISTER: false' config.yaml
require 'ENABLE_PIN: true' config.yaml
require 'ENABLE_PIC: true' config.yaml
reject 'MYSQL_ALLOW_EMPTY_PASSWORD|3307:3306|7577:7577' docker-compose.yml
reject '161-admin-data.sql' src/main/resources/db/changelog-data.xml
reject 'VALUES \(.admin.|Password: .admin.' src/main/resources
reject 'MYSQL_ROOT_PASSWORD: ""' docker-compose.yml

TAILSCALE_IP=100.64.0.10 LEOMS_BIND_IP=127.0.0.1 RESTIC_REPOSITORY=s3:https://example.invalid/leoms \
  docker compose --profile local config --quiet

[ "$failed" -eq 0 ] || exit 1
echo 'LeoMS configuration invariants passed.'
