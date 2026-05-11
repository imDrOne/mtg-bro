# Plan: Infisical Self-Hosted Integration

## Context

Secrets currently live in GitHub Environments. Every change requires a full re-deploy (tag → GitHub Actions → JIB build → migrate → docker compose pull). During active development with frequent env additions this is slow and tedious. Goal: self-hosted Infisical as the single source of truth. On any secret change in Infisical → `.env` file on server updates → container restarts in ~5s without a redeploy.

---

## Target Architecture

```
Admin access: ssh -L 8090:127.0.0.1:8090 user@server → localhost:8090 in browser
    │
    │ (Infisical never exposed publicly — no Caddy entry)
    ▼
Infisical (127.0.0.1:8090 on server, http://infisical:8080 on Docker network)
    │
    │ polls every ~60s
    ▼
infisical-agent container
    │  renders Go templates
    ├──► /opt/mtg-bro/docker/.collection-manager.env
    ├──► /opt/mtg-bro/docker/.draftsim-parser.env
    ├──► /opt/mtg-bro/docker/.wizard-stat-aggregator.env
    ├──► /opt/mtg-bro/docker/.auth-service.env
    ├──► /opt/mtg-bro/docker/.mcp-server.env
    └──► /opt/mtg-bro/docker/.infra.env
         then execs: docker compose up -d --no-deps --force-recreate <module>

GitHub Actions (tag deploy)
    └── SSH to server → docker exec infisical infisical export → .env file
        deploy step: docker compose pull <module> && up -d --no-deps <module>
```

**Infisical stays 100% internal.** No public URL, no Caddy entry. Admin uses SSH tunnel. CI also runs `infisical export` ON the server via `docker exec infisical` — no outbound connection from GitHub Actions runner to Infisical needed.

**Note on `--force-recreate` vs `docker restart`:** `docker restart` re-uses the existing process without re-reading `env_file` from disk. Only `up --force-recreate` stops and recreates the container, causing Docker to re-read `.env` file. Spring Boot cannot hot-reload `@Value` at runtime.

**Note on exec commands:** Docker socket mounted from host → docker CLI inside agent uses HOST paths for `-f` flag, not container mount paths. All compose `-f` flags must reference `/opt/mtg-bro/docker/docker-compose.prod.yml` (host path), not `/secrets/docker-compose.prod.yml` (mount path).

---

## Files to Create

| Path | Purpose |
|------|---------|
| `docker/infisical/docker-compose.yml` | Infisical app + dedicated postgres + redis |
| `docker/infisical/agent/Dockerfile` | infisical/cli + docker-cli (exec needs docker in agent container) |
| `docker/infisical/agent/docker-compose.yml` | Agent container |
| `docker/infisical/agent/agent-config.yaml` | Template + exec per module (7 blocks) |
| `docker/infisical/agent/templates/collection-manager.env.tpl` | Go template |
| `docker/infisical/agent/templates/draftsim-parser.env.tpl` | Go template |
| `docker/infisical/agent/templates/wizard-stat-aggregator.env.tpl` | Go template |
| `docker/infisical/agent/templates/auth-service.env.tpl` | Go template |
| `docker/infisical/agent/templates/mcp-server.env.tpl` | Go template |
| `docker/infisical/agent/templates/infra.env.tpl` | Go template |
| `.github/workflows/deploy-infisical.yml` | CI workflow for Infisical upgrades (tag: `infisical/v*`) |

## Files to Modify

| Path | Change |
|------|--------|
| `.github/workflows/_deploy-module.yml` | Replace `write-env.py` step with `docker exec infisical infisical export` in both `run-migrations` and `deploy` jobs |
| `.github/workflows/deploy-caddy.yml` | Same: replace `write-env.py` step (`--path /infra`) |
| `docs/deploy.md` | Update secrets management section; add SSH tunnel access instructions |
| `scripts/setup-server.sh` | Add `mkdir -p` for infisical dirs |

## Server-Side Files (never in git, written by CI or manually)

| Path | Written by |
|------|-----------|
| `/opt/mtg-bro/docker/infisical/.infisical.env` | `deploy-infisical.yml` workflow |
| `/opt/mtg-bro/docker/infisical/agent/.agent-auth.env` | Manual (one-time) |

---

## Implementation Steps

### Step 1: Infisical stack

`docker/infisical/docker-compose.yml`:
```yaml
services:
  infisical:
    image: infisical/infisical:latest
    container_name: infisical
    restart: unless-stopped
    env_file: [.infisical.env]
    depends_on:
      infisical-postgres:
        condition: service_healthy
      infisical-redis:
        condition: service_started
    ports:
      - "127.0.0.1:8090:8080"
    networks:
      - mtg-bro-net

  infisical-postgres:
    image: postgres:16-alpine
    container_name: infisical-postgres
    restart: unless-stopped
    environment:
      POSTGRES_USER: infisical_user
      POSTGRES_PASSWORD: ${INFISICAL_POSTGRES_PASSWORD}
      POSTGRES_DB: infisical
    volumes:
      - infisical_postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U infisical_user -d infisical"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - mtg-bro-net

  infisical-redis:
    image: redis:7-alpine
    container_name: infisical-redis
    restart: unless-stopped
    volumes:
      - infisical_redis_data:/data
    networks:
      - mtg-bro-net

networks:
  mtg-bro-net:
    external: true

volumes:
  infisical_postgres_data:
  infisical_redis_data:
```

Separate postgres/redis from app postgres: blast radius isolation + independent upgrade/restore lifecycle.

---

### Step 2: Agent Dockerfile

`docker/infisical/agent/Dockerfile`:
```dockerfile
FROM infisical/cli:latest
RUN apk add --no-cache docker-cli
```

Agent needs docker CLI to exec compose commands. `infisical/cli` base is Alpine-based.

---

### Step 3: Agent config

`docker/infisical/agent/agent-config.yaml`:
```yaml
infisical:
  address: "http://infisical:8080"

auth:
  type: "universal-auth"
  config:
    client-id: "${INFISICAL_AGENT_CLIENT_ID}"
    client-secret: "${INFISICAL_AGENT_CLIENT_SECRET}"

sinks:
  - type: "file"
    config:
      path: "/tmp/agent-token"

templates:
  - source-path: /templates/collection-manager.env.tpl
    destination-path: /secrets/.collection-manager.env
    exec:
      command: >
        docker compose -f /opt/mtg-bro/docker/docker-compose.prod.yml
        up -d --no-deps --force-recreate collection-manager
      timeout: 60

  - source-path: /templates/draftsim-parser.env.tpl
    destination-path: /secrets/.draftsim-parser.env
    exec:
      command: >
        docker compose -f /opt/mtg-bro/docker/docker-compose.prod.yml
        up -d --no-deps --force-recreate draftsim-parser
      timeout: 60

  - source-path: /templates/wizard-stat-aggregator.env.tpl
    destination-path: /secrets/.wizard-stat-aggregator.env
    exec:
      command: >
        docker compose -f /opt/mtg-bro/docker/docker-compose.prod.yml
        up -d --no-deps --force-recreate wizard-stat-aggregator
      timeout: 60

  - source-path: /templates/auth-service.env.tpl
    destination-path: /secrets/.auth-service.env
    exec:
      command: >
        docker compose -f /opt/mtg-bro/docker/docker-compose.prod.yml
        up -d --no-deps --force-recreate auth-service
      timeout: 60

  - source-path: /templates/mcp-server.env.tpl
    destination-path: /secrets/.mcp-server.env
    exec:
      command: >
        docker compose -f /opt/mtg-bro/docker/docker-compose.prod.yml
        up -d --no-deps --force-recreate mcp-server
      timeout: 60

  - source-path: /templates/infra.env.tpl
    destination-path: /secrets/.infra.env
    exec:
      command: >
        docker compose -f /opt/mtg-bro/docker/docker-compose.prod.yml
        up -d --no-deps caddy
      timeout: 60
```

Note: caddy uses `build: ./caddy` in compose — `--force-recreate` without rebuild is safe for env-only changes. Image doesn't change, only env vars.

---

### Step 4: Agent docker-compose

`docker/infisical/agent/docker-compose.yml`:
```yaml
services:
  infisical-agent:
    build: .
    container_name: infisical-agent
    restart: unless-stopped
    command: agent --config /agent-config.yaml
    env_file: [.agent-auth.env]
    environment:
      GHCR_OWNER: ${GHCR_OWNER}
    volumes:
      - ./agent-config.yaml:/agent-config.yaml:ro
      - ./templates:/templates:ro
      - /opt/mtg-bro/docker:/secrets:rw
      - /var/run/docker.sock:/var/run/docker.sock
    networks:
      - mtg-bro-net

networks:
  mtg-bro-net:
    external: true
```

`GHCR_OWNER` needed so docker-compose.prod.yml can resolve `ghcr.io/${GHCR_OWNER}/...` image names when compose CLI parses the file during `up`.

`.agent-auth.env` (server-side, manual):
```
INFISICAL_AGENT_CLIENT_ID=<from Infisical Machine Identity>
INFISICAL_AGENT_CLIENT_SECRET=<from Infisical Machine Identity>
INFISICAL_URL=http://infisical:8080
GHCR_OWNER=<github org name lowercase>
```

---

### Step 5: Go templates

**Шаблоны итерируют ВСЕ секреты по пути** — при добавлении нового env в Infisical UI шаблон трогать не нужно:

`collection-manager.env.tpl` (и все остальные — одинаковый паттерн, только путь меняется):
```
{{- with secret "mtg-bro" "production" "/collection-manager" -}}
{{- range .Secrets }}
{{ .Key }}={{ .Value }}
{{- end }}
{{- end }}
```

Добавил переменную в Infisical → агент автоматически видит её при следующем poll → пишет в `.env` → перезапускает контейнер. Никаких правок в репо.

Аналогично `infisical export` в CI — дампит весь путь целиком без явного перечисления ключей.

6 шаблонов, отличаются только путём: `/collection-manager`, `/draftsim-parser`, `/wizard-stat-aggregator`, `/auth-service`, `/mcp-server`, `/infra`.

---

### Step 6: Admin access (SSH tunnel, no server changes needed)

Infisical port `127.0.0.1:8090:8080` in compose → only accessible on loopback.

Access UI:
```bash
ssh -L 8090:127.0.0.1:8090 drone@<server-ip> -N
# then open http://localhost:8090 in browser
```

No Caddy changes. No new GitHub Environment secrets. Nothing public.

---

### Step 7: deploy-infisical.yml

New workflow for Infisical self-upgrades (tag: `infisical/v*`):
```yaml
on:
  push:
    tags: ['infisical/v*']

jobs:
  deploy:
    runs-on: ubuntu-latest
    environment: production-infisical
    permissions:
      contents: read
    steps:
      - uses: actions/checkout@v4
      - name: Decode SSH key
        run: |
          echo "${{ secrets.SSH_PRIVATE_KEY }}" | base64 -d > /tmp/ssh_key
          chmod 600 /tmp/ssh_key
      - name: Write Infisical bootstrap env to server
        env:
          ALL_SECRETS: ${{ toJSON(secrets) }}
        run: |
          python3 .github/scripts/write-env.py
          scp -i /tmp/ssh_key -o StrictHostKeyChecking=no \
            /tmp/module.env \
            ${{ secrets.SSH_USER }}@${{ secrets.SSH_HOST }}:/opt/mtg-bro/docker/infisical/.infisical.env
      - name: Restart Infisical
        uses: appleboy/ssh-action@v1
        with:
          host: ${{ secrets.SSH_HOST }}
          username: ${{ secrets.SSH_USER }}
          key_path: /tmp/ssh_key
          script: |
            cd /opt/mtg-bro/docker/infisical
            docker compose pull infisical
            docker compose up -d --no-deps --force-recreate infisical
```

`production-infisical` GitHub Environment secrets:
```
SSH_HOST, SSH_USER, SSH_PRIVATE_KEY  (standard CI mechanics)
ENCRYPTION_KEY   # openssl rand -hex 16
AUTH_SECRET      # openssl rand -base64 32
DB_CONNECTION_URI=postgresql://infisical_user:<pass>@infisical-postgres:5432/infisical
REDIS_URL=redis://infisical-redis:6379
SITE_URL=http://localhost:8090
INFISICAL_POSTGRES_PASSWORD=<strong password>
```

---

### Step 8: Update _deploy-module.yml

Replace `write-env.py` steps in **both** `run-migrations` and `deploy` jobs.

CI runs `infisical export` **on the server** via `docker exec` — Infisical never needs a public URL:

```yaml
- name: Write env file from Infisical
  uses: appleboy/ssh-action@v1
  with:
    host: ${{ secrets.SSH_HOST }}
    username: ${{ secrets.SSH_USER }}
    key_path: /tmp/ssh_key
    envs: MODULE,INFISICAL_CLIENT_ID,INFISICAL_CLIENT_SECRET
    script: |
      docker exec infisical infisical export \
        --domain http://localhost:8080 \
        --env production \
        --path /$MODULE \
        --format dotenv \
        --clientId "$INFISICAL_CLIENT_ID" \
        --clientSecret "$INFISICAL_CLIENT_SECRET" \
        > /opt/mtg-bro/docker/.$MODULE.env
  env:
    MODULE: ${{ inputs.module }}
    INFISICAL_CLIENT_ID: ${{ secrets.INFISICAL_CLIENT_ID }}
    INFISICAL_CLIENT_SECRET: ${{ secrets.INFISICAL_CLIENT_SECRET }}
```

`docker exec infisical` — запускает Infisical CLI внутри уже запущенного контейнера, который видит себя на `localhost:8080`. Никакой установки CLI на хост не нужно.

Add to each module's GitHub Environment: `INFISICAL_CLIENT_ID`, `INFISICAL_CLIENT_SECRET`.

Remove from each module's GitHub Environment: all app-specific secrets (DB_*, API keys, etc.) after migration verified.

Keep in each module's GitHub Environment: `SSH_HOST`, `SSH_USER`, `SSH_PRIVATE_KEY`, `GHCR_TOKEN`.

---

### Step 9: Update deploy-caddy.yml

Same replacement: `write-env.py` → `infisical export --path=/infra`.

---

## Infisical Project Layout

```
Project: mtg-bro
Environment: production
Paths:
  /collection-manager/*
  /draftsim-parser/*
  /wizard-stat-aggregator/*
  /auth-service/*
  /mcp-server/*
  /infra/*
```

Machine Identities (Universal Auth):
1. `mtg-bro-agent` — read `/**` — credentials in `.agent-auth.env`
2. `ci-collection-manager` — read `/collection-manager/*` — credentials in GH Environment
3. `ci-draftsim-parser` — read `/draftsim-parser/*`
4. `ci-wizard-stat-aggregator` — read `/wizard-stat-aggregator/*`
5. `ci-auth-service` — read `/auth-service/*`
6. `ci-mcp-server` — read `/mcp-server/*`
7. `ci-infra` — read `/infra/*`

---

## Migration Order (phased, one module at a time)

0. Deploy code changes (docker files, workflows) — no secrets touched yet
1. Spin up Infisical manually on VPS (`docker compose up -d` in `docker/infisical/`)
2. Initial UI setup: org → project `mtg-bro` → environment `production`
3. **Pilot: mcp-server** — simplest (5 vars, no DB migrations)
   - Populate `/mcp-server/*` secrets in Infisical UI
   - Create `mtg-bro-agent` machine identity, write `.agent-auth.env` to server manually
   - Start agent with only mcp-server template uncommented
   - Verify: change a var in UI → check file updated → check container restarted
4. Add remaining modules one by one:
   - `wizard-stat-aggregator` (4 vars)
   - `auth-service` (8 vars)
   - `collection-manager` (5 vars)
   - `draftsim-parser` (14 vars — most complex, do last)
   - `infra` (Caddy vars — do last, affects routing)
5. For each module: create `ci-<module>` machine identity → add to GH Environment → test tag deploy → remove old app secrets from GH Environment

---

## Verification

1. `docker logs infisical` — no errors; accessible via SSH tunnel at `localhost:8090`
2. `docker exec infisical-postgres pg_isready` — `accepting connections`
3. `docker logs infisical-agent` — shows template render cycle, no auth errors
4. Change a var in Infisical UI → within 60s:
   - `cat /opt/mtg-bro/docker/.<module>.env` — new value present
   - `docker inspect <module> --format='{{.State.StartedAt}}'` — recent timestamp
   - `docker exec <module> env | grep <CHANGED_VAR>` — new value active in process
5. Tag push triggers GitHub Actions → `infisical export` step logs show secrets fetched → deploy succeeds
6. Negative: `docker exec mcp-server env | grep DB_HOST` — empty (no DB secrets leaked across paths)
