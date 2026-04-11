#!/bin/bash
# Server infrastructure setup script for mtg-bro.
# Run as the deploy user (e.g. drone) with sudo privileges.
# Usage: bash setup-server.sh

set -e

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo "==> Creating directories..."
sudo mkdir -p /opt/mtg-bro/docker
sudo mkdir -p /opt/mtg-bro/postgres/data
sudo chown -R "$USER:$USER" /opt/mtg-bro

echo "==> Creating Docker network..."
docker network inspect mtg-bro-net >/dev/null 2>&1 \
  && echo "    mtg-bro-net already exists, skipping" \
  || docker network create mtg-bro-net

echo "==> Copying docker files to server..."
cp -r "$REPO_ROOT/docker/." /opt/mtg-bro/docker/

echo "==> Generating SSH deploy key for GitHub Actions..."
KEY_PATH="$HOME/.ssh/deploy_key"
if [ -f "$KEY_PATH" ]; then
  echo "    $KEY_PATH already exists, skipping key generation"
else
  ssh-keygen -t ed25519 -C "github-actions" -f "$KEY_PATH" -N ""
  cat "$KEY_PATH.pub" >> "$HOME/.ssh/authorized_keys"
  chmod 600 "$HOME/.ssh/authorized_keys"
  echo ""
  echo "    ✓ Public key added to authorized_keys"
fi

echo ""
echo "==> Done. Next steps:"
echo ""
echo "  1. Start PostgreSQL (set real credentials):"
echo "     DB_USERNAME=<user> DB_PASSWORD=<password> \\"
echo "       docker compose -f /opt/mtg-bro/docker/postgres/docker-compose.yml up -d"
echo ""
echo "  2. Add the following secret to GitHub Actions (Settings → Secrets → Actions):"
echo "     SSH_PRIVATE_KEY (base64):"
echo ""
cat "$HOME/.ssh/deploy_key" | base64 -w 0
echo ""
echo ""
echo "     SSH_HOST: $(curl -s ifconfig.me 2>/dev/null || echo '<server-ip>')"
echo "     SSH_USER: $USER"
