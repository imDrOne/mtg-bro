#!/bin/bash
# Syncs the local docker/ directory to /opt/mtg-bro/docker on the server.
# Run as the deploy user with write access to /opt/mtg-bro.
# Usage: bash update-docker-config.sh

set -e

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TARGET="/opt/mtg-bro/docker"

if [ ! -d "$TARGET" ]; then
  echo "ERROR: $TARGET does not exist. Run setup-server.sh first." >&2
  exit 1
fi

echo "==> Syncing $REPO_ROOT/docker/ -> $TARGET ..."
rsync -av --delete "$REPO_ROOT/docker/" "$TARGET/"

echo "==> Done."
