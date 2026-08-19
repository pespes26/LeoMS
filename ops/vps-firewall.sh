#!/bin/sh
set -eu

[ "$(id -u)" -eq 0 ] || { echo 'Run as root on the VPS.' >&2; exit 1; }
ip link show tailscale0 >/dev/null 2>&1 || { echo 'tailscale0 is not present.' >&2; exit 1; }

iptables -N LEOMS-INGRESS 2>/dev/null || true
iptables -F LEOMS-INGRESS
iptables -A LEOMS-INGRESS -i tailscale0 -p tcp -m multiport --dports 8484,7575,7576 -j ACCEPT
iptables -A LEOMS-INGRESS -p tcp -m multiport --dports 8484,7575,7576 -j DROP
iptables -A LEOMS-INGRESS -j RETURN

iptables -C DOCKER-USER -j LEOMS-INGRESS 2>/dev/null || iptables -I DOCKER-USER 1 -j LEOMS-INGRESS

echo 'LeoMS Docker ingress now accepts game ports only from tailscale0.'
echo 'Persist the verified rules with your distribution firewall tooling before rebooting.'
