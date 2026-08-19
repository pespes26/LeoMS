# LeoMS

LeoMS is a private, non-commercial MapleStory v83 server for a small group of friends. It runs one Scania world with two channels, 2× EXP, and 1× meso, drop, boss-drop, and quest rates. Access is restricted to an explicitly approved Tailscale group.

This repository is derived from [Cosmic v1.1.3](https://github.com/P0nk/Cosmic/tree/v1.1.3) at commit `fec53bc7714dc0f1ae3f50b2986cdf2727e0912a`. Cosmic and LeoMS are licensed under [AGPL-3.0](LICENSE); Cosmic’s authors and contributors retain their attribution. The complete upstream history is preserved and the `upstream` Git remote points to `https://github.com/P0nk/Cosmic.git`.

No client installer, executable, WZ binary, archive, credential, or other proprietary client material belongs in this repository. Players must prepare their own legitimately obtained v83 files by following [the client guide](docs/CLIENT_PREPARATION.md).

## Architecture

- `maplestory`: the minimally changed Java 21 Cosmic game server.
- `db`: MySQL 8.4 with separate game, admin, backup, and root credentials.
- `admin`: a Java 21 Spring Boot/Thymeleaf operator UI, reachable only through Tailscale Serve.
- `gateway`: a non-root TCP proxy that is the only service attached to a host-facing network.
- `backup`: an isolated logical-dump/restic worker with no Docker socket.

MySQL has no host-published port. On Linux, game ports bind only to `TAILSCALE_IP`; on macOS they bind to loopback and use Tailscale Serve TCP forwarding. The admin binds only to host loopback and uses HTTPS supplied by Tailscale Serve. Database data, game logs, and backup cache use persistent named volumes.

## Local start

Prerequisites: Docker Desktop, Tailscale, a stable tailnet IPv4 for the host, and a private S3-compatible restic destination.

```sh
cp .env.example .env
./ops/generate-secrets.sh
# edit .env with the real tailnet IP and restic repository; keep
# LEOMS_BIND_IP=127.0.0.1 on macOS
docker compose --profile local config --quiet
docker compose --profile local up --build -d
tailscale serve --bg http://127.0.0.1:8080
tailscale serve --bg --tcp=8484 tcp://127.0.0.1:8484
tailscale serve --bg --tcp=7575 tcp://127.0.0.1:7575
tailscale serve --bg --tcp=7576 tcp://127.0.0.1:7576
```

The secret generator asks for the admin password and S3 credentials without echoing them. It stores only a bcrypt cost-12 hash for the admin password. Do not reuse the admin password for a game account.

Open the HTTPS URL printed by `tailscale serve`. Create all game accounts in the admin panel; automatic registration is disabled and a clean database contains no account credentials.

See [Operations](docs/OPERATIONS.md), [Tailscale access](docs/TAILSCALE.md), [VPS deployment](docs/VPS_DEPLOYMENT.md), and [testing](docs/TESTING.md) before inviting players or promoting staging.

## Development checks

```sh
./mvnw test
docker build -t leoms-game:test .
docker build -t leoms-admin:test admin
./ops/verify-config.sh
./ops/scan-repository.sh
```

The host needs Java 21 for `./mvnw test`; the Docker builds provide their own Java toolchains. MySQL integration tests in the admin project run automatically when a Docker daemon is available and otherwise report as skipped.

## Scope and safety

LeoMS v1 intentionally has no destructive character, inventory, meso, level, or item editor. Gameplay intervention stays in audited in-game GM commands. It has no public registration, donations, paid features, public API, or public network listener. Any expansion of that scope needs a new security and legal review.
