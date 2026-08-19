# Operator runbook

Run commands from the repository root. Use `--profile local` on staging and `--profile vps` on the VPS.

## First admin and initial start

1. Copy `.env.example` to `.env` and set the stable Tailscale IPv4 and private restic repository.
2. Run `./ops/generate-secrets.sh`. The first admin identity is `LEOMS_ADMIN_USERNAME`; its password exists only in your password manager, while the secret file contains a bcrypt cost-12 hash.
3. Start with `docker compose --profile local up --build -d`.
4. Wait for `docker compose --profile local ps` to show all services healthy.
5. Run `tailscale serve --bg http://127.0.0.1:8080` and sign into the tailnet HTTPS URL.
6. Create a non-GM game account in the panel. No account is seeded and unknown usernames cannot self-register.

PIN and PIC remain enabled. Treat the panel password, database secrets, restic password, and S3 keys as separate credentials. To rotate a database service credential, replace its secret file and recreate `db` plus the dependent service; the database-side grant watcher applies the new admin and backup passwords after migrations. The game user is managed by the official MySQL entrypoint and must be rotated explicitly in MySQL before replacing its file.

## Start and stop

```sh
docker compose --profile local up -d
docker compose --profile local ps
docker compose --profile local logs --tail=200 maplestory admin backup db
docker compose --profile local stop maplestory admin backup
docker compose --profile local stop db
```

Start the database before the game; Compose health dependencies do this automatically. For shutdown, stop services that write to MySQL before stopping MySQL. Never delete volumes as part of routine shutdown.

## Account recovery

Search by account or character name in the panel. Reset only the requested password/PIN/PIC fields and send new values through a separate trusted channel. The audit row records which credential classes changed, never their values. Ban compromised accounts with a concise reason, investigate, reset credentials, then unban.

The panel deliberately cannot edit characters, inventory, meso, level, or items. Use established in-game GM commands for gameplay intervention and retain the relevant game logs.

## Backups

The worker performs a MySQL logical dump at least every six hours and whenever the panel queues a request. It encrypts snapshots with restic before upload. Retention is 7 days of six-hour snapshots, 30 daily snapshots, and 8 weekly snapshots. A failed dump/upload marks the job failed and keeps the worker alive for later requests.

Before maintenance, request a backup and wait for `SUCCEEDED`. Inspect worker logs if it fails:

```sh
docker compose --profile local logs --tail=200 backup
docker compose --profile local run --rm backup backup-now
```

## Restore and monthly restore test

Never test a restore over the live database. Run `./ops/monthly-restore-test.sh local`; it restores the latest encrypted snapshot into a disposable MySQL container/volume, verifies key tables and row counts, and then removes only its explicitly named temporary resources. Record the date and result in the operator log. Run the same test before VPS promotion.

For an actual recovery, stop game/admin/backup, move the current `mysql-data` volume aside rather than deleting it, start an empty database with the same schema name, restore the selected restic dump, then start the game so Liquibase can apply any later migrations. Validate accounts, characters, audit rows, and backup jobs before reopening access. The monthly script is the executable reference; use `RESTIC_SNAPSHOT` to select a non-latest snapshot.

## Incident response

1. Contain: remove the affected friend from the Tailscale group. For wider compromise, stop game/admin and disable Tailscale Serve.
2. Preserve: do not prune volumes or logs; record UTC time, observed IP/device, accounts, and symptoms. Request a backup only if doing so will not overwrite needed evidence.
3. Assess: review bounded panel logs, full Docker volume logs, account audit rows, Tailscale device activity, and S3 access logs.
4. Recover: rotate affected credentials, revoke Tailscale keys/devices, restore only from a verified snapshot if data integrity is uncertain, and validate on staging.
5. Reopen gradually: owner admin first, then one player, then the approved group. Document root cause and preventive changes.

## Upstream upgrades

Never rebase directly onto an untested upstream tip.

```sh
git fetch upstream --tags
upgrade_tree=$(mktemp -d /tmp/leoms-upstream.XXXXXX)
git worktree add "$upgrade_tree" upstream/master
(cd "$upgrade_tree" && ./mvnw test && docker build -t cosmic-upstream-candidate .)
git worktree remove "$upgrade_tree"
git rebase upstream/master
./mvnw test
docker build -t leoms-game:test .
./ops/verify-config.sh
./ops/scan-repository.sh
```

Review every upstream migration, especially seeded data and changes to `accounts`. Reconfirm that automatic registration is false, no credentials are seeded, LeoMS migrations remain prefixed, and the three published game ports have not expanded. Restore-test a fresh backup and complete the gameplay checklist before deployment.
