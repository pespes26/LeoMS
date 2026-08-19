# Tailscale access policy

Use a dedicated server tag and explicit identity groups. Do not share one tailnet login among friends. Keep device approval enabled, use expiring keys for ordinary devices, and review the device list when membership changes.

The example below uses Tailscale's ACL policy syntax. Replace every example identity, merge it with the tailnet's existing policy, and validate it in the Tailscale admin console before saving. The owner is intentionally not included in the friends group so each permission is visible.

```json
{
  "groups": {
    "group:leoms-owner": ["owner@example.com"],
    "group:leoms-friends": [
      "friend-one@example.com",
      "friend-two@example.com"
    ]
  },
  "tagOwners": {
    "tag:leoms-server": ["group:leoms-owner"]
  },
  "acls": [
    {
      "action": "accept",
      "src": ["group:leoms-friends"],
      "dst": ["tag:leoms-server:8484,7575-7576"]
    },
    {
      "action": "accept",
      "src": ["group:leoms-owner"],
      "dst": ["tag:leoms-server:443,8484,7575-7576"]
    }
  ]
}
```

Tag the server node `tag:leoms-server`. Confirm its stable IPv4 with `tailscale ip -4`, put that value in `.env`, and do not use a public IP or MagicDNS hostname as Cosmic's advertised host. The Docker mappings bind game listeners to that exact IPv4.

On a Linux/VPS host, set `LEOMS_BIND_IP` to the same stable Tailscale IPv4. Docker Desktop cannot reliably publish directly onto a macOS Tailscale interface, so macOS staging uses `LEOMS_BIND_IP=127.0.0.1` and private Tailscale TCP forwarding:

```sh
tailscale serve --bg --tcp=8484 tcp://127.0.0.1:8484
tailscale serve --bg --tcp=7575 tcp://127.0.0.1:7575
tailscale serve --bg --tcp=7576 tcp://127.0.0.1:7576
```

These are tailnet-only Serve listeners, not public Funnel listeners. Cosmic still advertises `TAILSCALE_IP` to clients.

Publish the loopback-only admin app with Tailscale Serve:

```sh
tailscale serve --bg http://127.0.0.1:8080
tailscale serve status
```

This gives the browser tailnet HTTPS on port 443 while the container remains unreachable on any host interface. The ACL grants port 443 only to the owner. If the Serve CLI on an older installed client uses different syntax, follow the version-matched official `tailscale serve --help`; do not compensate by publishing container port 8080 publicly.

After every policy change, test from three positions: the owner can reach HTTPS admin and all game ports; an approved friend can reach only `8484`, `7575`, and `7576`; an unapproved tailnet identity and a public-internet host can reach none of them.
