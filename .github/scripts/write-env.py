"""
Reads ALL_SECRETS (JSON from toJSON(secrets)) and writes a .env file to /tmp/module.env,
excluding CI-mechanics secrets that should not end up on the server.
"""
import json
import os

secrets = json.loads(os.environ["ALL_SECRETS"])

CI_SECRETS = {"GITHUB_TOKEN", "SSH_HOST", "SSH_USER", "SSH_PRIVATE_KEY", "GHCR_TOKEN"}

lines = "\n".join(f"{k}={v}" for k, v in secrets.items() if k not in CI_SECRETS)

with open("/tmp/module.env", "w") as f:
    f.write(lines + "\n")
