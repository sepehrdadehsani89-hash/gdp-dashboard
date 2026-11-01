#!/usr/bin/env bash
# Run locally/in a private terminal, never place the output in a public repository.
set -euo pipefail
umask 077
: "${JARVIS_NEXT_SIGNING_DIR:?Set to the private signing directory}"
output=${1:?Usage: backup-next-signing.sh /private/path/jarvis-next-signing.tar.enc}
repo=$(cd "$(dirname "$0")/.." && pwd -P)
output=$(realpath -m "$output")
case "$output" in "$repo"|"$repo"/*) echo 'Refusing to write a signing backup inside the repository.' >&2; exit 1;; esac
[[ ! -e "$output" ]] || { echo 'Refusing to overwrite an existing backup.' >&2; exit 1; }
[[ -f "$JARVIS_NEXT_SIGNING_DIR/jarvis-next.p12" && -f "$JARVIS_NEXT_SIGNING_DIR/store-password" ]]
# OpenSSL asks privately for a NEW backup-encryption password. Keep it in a password manager.
# Restore to a private directory using openssl enc -d with the same cipher/PBKDF2/iteration options,
# then extract the tar there. Verify the public certificate fingerprint before signing an update.
tar -C "$JARVIS_NEXT_SIGNING_DIR" -czf - jarvis-next.p12 store-password |
    openssl enc -aes-256-cbc -salt -pbkdf2 -iter 200000 -out "$output"
chmod 600 "$output"
printf 'Encrypted private backup saved: %s\n' "$output"
