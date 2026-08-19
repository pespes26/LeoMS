# Validation checklist

## Automated

```sh
./mvnw test
docker build -t leoms-game:test .
docker build -t leoms-admin:test admin
./ops/verify-config.sh
./ops/scan-repository.sh
```

The admin suite covers credential validation, bcrypt, duplicate usernames, resets, bans, CSRF authorization, audit redaction, and login throttling. Its MySQL Testcontainers class runs when a Docker daemon is available. Run clean Compose integration separately because the game needs its large server-side WZ XML data.

## Clean startup and negative cases

Use a new Compose project name or explicitly new named volumes; never delete the active staging/production database for a test. Confirm migrations finish, the `accounts` table is empty, and `leoms_admin_audit` plus `leoms_backup_jobs` exist. Confirm server logs report one world and channels `7575`/`7576`, and that no `7577` listener exists.

Try a nonexistent username and verify it is rejected without inserting an account. Create a player in the panel and verify password, PIN, and PIC login. Test duplicate creation, password/PIN/PIC resets, ban/unban, five failed panel logins, session expiry, missing CSRF, a second simultaneous backup request, an unreadable/empty secret, invalid S3 credentials, database restart, and game restart.

## Gameplay

Test on a physical Windows 10/11 machine and a Windows VM on macOS, with Tailscale inside each. Cover login, character creation, channel switching, quests, party play, trading, cash shop, PIN/PIC, forced disconnect, and reconnect. Record defects by client hash without uploading client files.

For capacity, connect ten clients for at least one hour. Exercise both channels and shared activities. Monitor `docker stats`, game logs, MySQL health, JVM restarts, connection counts, disk growth, and latency. Acceptance requires no unplanned restart, database error, persistent login failure, or resource trend that would exhaust the 4 GB VPS baseline.

## Backup, restore, and network

Request a manual backup, simulate one failed upload using a deliberately invalid test-only S3 endpoint, restore valid configuration, and verify the next request succeeds. Run `./ops/monthly-restore-test.sh local` and inspect its critical-table and row-count result.

Test reachability as described in [Tailscale access](TAILSCALE.md) and [VPS deployment](VPS_DEPLOYMENT.md). Public scans must include IPv4 and IPv6. Run the repository/history scan immediately before every push.
