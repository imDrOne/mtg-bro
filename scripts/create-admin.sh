#!/usr/bin/env bash
set -euo pipefail

echo "=== mtg-bro Admin Bootstrap ==="
echo ""

# Parse CLI args
ADMIN_EMAIL=""
ADMIN_USERNAME=""
ADMIN_PASSWORD=""

while [[ $# -gt 0 ]]; do
    case "$1" in
        --email)    ADMIN_EMAIL="$2";    shift 2 ;;
        --username) ADMIN_USERNAME="$2"; shift 2 ;;
        --password) ADMIN_PASSWORD="$2"; shift 2 ;;
        -h|--help)
            echo "Usage: $0 [--email EMAIL] [--username USERNAME] [--password PASSWORD]"
            exit 0 ;;
        *) echo "Unknown argument: $1" >&2; exit 1 ;;
    esac
done

# Fall back to interactive if not provided via args
[[ -z "$ADMIN_EMAIL" ]]    && read -rp "Email: " ADMIN_EMAIL
[[ -z "$ADMIN_USERNAME" ]] && read -rp "Username: " ADMIN_USERNAME
[[ -z "$ADMIN_PASSWORD" ]] && { read -rsp "Password: " ADMIN_PASSWORD; echo ""; }

if [[ -z "$ADMIN_EMAIL" || -z "$ADMIN_USERNAME" || -z "$ADMIN_PASSWORD" ]]; then
    echo "Error: all fields are required"
    exit 1
fi

# Generate bcrypt hash
if command -v python3 &>/dev/null && python3 -c "import bcrypt" 2>/dev/null; then
    HASH=$(python3 -c "
import bcrypt, sys
password = sys.argv[1].encode()
hashed = bcrypt.hashpw(password, bcrypt.gensalt(12))
print(hashed.decode())
" "$ADMIN_PASSWORD")
elif command -v htpasswd &>/dev/null; then
    HASH=$(htpasswd -bnBC 12 "" "$ADMIN_PASSWORD" | tr -d ':\n' | sed 's/^\$2y\$/\$2a\$/')
else
    echo ""
    echo "Error: neither python3 bcrypt module nor htpasswd is available."
    echo "Install one of:"
    echo "  pip3 install bcrypt"
    echo "  brew install httpd  # macOS"
    echo "  apt-get install apache2-utils  # Ubuntu"
    exit 1
fi

echo ""
echo "=== SQL Commands (execute in psql) ==="
echo ""
cat <<SQL
BEGIN;

INSERT INTO users (email, username, password_hash, enabled)
VALUES ('${ADMIN_EMAIL}', '${ADMIN_USERNAME}', '${HASH}', true);

INSERT INTO user_roles (user_id, role)
SELECT id, 'ADMIN' FROM users WHERE email = '${ADMIN_EMAIL}';

COMMIT;

-- Verify:
SELECT u.id, u.email, u.username, u.enabled, ur.role
FROM users u
JOIN user_roles ur ON ur.user_id = u.id
WHERE u.email = '${ADMIN_EMAIL}';
SQL

echo ""
echo "=== Connect to DB ==="
echo "  docker exec -it postgres psql -U wizard_user -d auth_service_db"
echo ""
echo "Paste the SQL above into the psql prompt."
