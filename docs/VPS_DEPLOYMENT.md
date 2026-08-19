# Ubuntu 24.04 VPS deployment

Choose a provider-neutral x86-64 Ubuntu 24.04 LTS host with at least 2 vCPUs, 4 GB RAM, and 40 GB SSD. Before purchasing a long commitment, measure latency from the actual players to candidate Israel and nearby Europe regions; choose the lowest stable median/p95 route, not the geographically closest label.

## Promotion gate

Do not promote until local staging has passed:

- Cosmic and admin tests plus both container builds.
- A clean database start with no accounts, one active world, and two channels at the configured rates.
- A successful encrypted backup and disposable restore test.
- The full hardware/VM gameplay checklist and a one-hour, ten-client soak.
- Database and container restart recovery, invalid-secret failure, failed-upload recovery, and duplicate backup rejection.
- Tailnet/public reachability scans and the repository/history safety scan.

Keep local Docker Desktop as staging after promotion.

## Host setup

Install Docker Engine from Docker's Ubuntu repository, the Compose plugin, Tailscale, and your normal OS security updates. Do not install MySQL on the host. Enroll the node with `tag:leoms-server`, approve it, and confirm `tailscale0` and the stable IPv4.

Clone LeoMS over an authenticated channel, create `.env`, and transfer freshly generated secrets through a secure channel. Prefer generating database/restic secrets directly on the VPS and copying only the admin bcrypt hash if the same panel password is required. Set file mode `0600` and restrict the checkout to the operator account.

Apply and inspect the Docker forwarding firewall after Docker has created `DOCKER-USER`:

```sh
sudo ./ops/vps-firewall.sh
sudo iptables -S LEOMS-INGRESS
sudo iptables -S DOCKER-USER
```

Persist the rules with the site's Ubuntu firewall tooling. Re-run verification after Docker/firewall upgrades and reboot tests. The Compose file also binds game ports only to `TAILSCALE_IP`, keeps admin on `127.0.0.1`, and publishes no database port; these are independent layers.

## Deploy

```sh
docker compose --profile vps config --quiet
docker compose --profile vps up --build -d
docker compose --profile vps ps
tailscale serve --bg http://127.0.0.1:8080
./ops/monthly-restore-test.sh vps
```

Check memory, disk, container health, JVM logs, database connections, and backup completion during the first gameplay session. Do not expose a temporary public port for troubleshooting. Use Tailscale SSH or the provider console.

## Network verification

From an approved friend device, scan only the server tailnet IP and expect `8484`, `7575`, and `7576`. Port 443 must be denied for friends and accepted for the owner. From a host outside the tailnet, scan both the VPS public IPv4 and IPv6; none of the game, admin, or database ports (`8484`, `7575`, `7576`, `8080`, `3306`) may answer. Also inspect `ss -lntp`: Docker mappings should show the tailnet IPv4 for game ports and loopback for admin.

If IPv6 is enabled publicly, confirm the provider firewall and host policy default-deny unsolicited IPv6. The Compose mappings contain no IPv6 publication, but that does not replace an IPv6 firewall review.
