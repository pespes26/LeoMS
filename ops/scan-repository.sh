#!/bin/sh
set -eu

repo_dir=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$repo_dir"
failed=0
forbidden='\.(exe|msi|dll|dmg|pkg|wz|zip|7z|rar|tar|tgz|gz|p12|pfx|jks|pem|key)$'

tracked=$(git ls-files | rg -i "$forbidden" || true)
if [ -n "$tracked" ]; then printf 'Forbidden tracked files:\n%s\n' "$tracked" >&2; failed=1; fi

# Cosmic's preserved history contains a deleted PCRE redistribution used by an old
# developer tool. It is not a MapleStory client asset and is absent from this tree.
historic=$(git log --all --name-only --pretty=format: | rg -i "$forbidden" \
  | rg -v '^tools/Script(QuestRelease|StaticMethod)Tracker/pcre3\.dll$' | sort -u || true)
if [ -n "$historic" ]; then printf 'Forbidden files found in Git history:\n%s\n' "$historic" >&2; failed=1; fi

secret_names='(^|/)(mysql_(root|game|admin|backup)_password|admin_password_hash|restic_password|s3_access_key|s3_secret_key|\.env)$'
tracked_secrets=$(git ls-files | rg "$secret_names" || true)
if [ -n "$tracked_secrets" ]; then printf 'Runtime secret files are tracked:\n%s\n' "$tracked_secrets" >&2; failed=1; fi

if command -v gitleaks >/dev/null 2>&1; then
  gitleaks git --no-banner . || failed=1
else
  echo 'Note: gitleaks is not installed; filename/history policy checks still ran.' >&2
fi

[ "$failed" -eq 0 ] || exit 1
echo 'Repository and Git-history safety scan passed.'
