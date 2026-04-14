Scan all module `application.yml` files and sync the environment variable tables in `docs/deploy.md`, each module's `CLAUDE.md`, and all scripts that reference module names or env vars.

## Steps

1. **Read current env vars from source of truth** for each module:
   - `collection-manager/src/main/resources/application.yml`
   - `draftsim-parser/src/main/resources/application.yml`
   - `wizard-stat-aggregator/src/main/resources/application.yml`
   - `auth-service/src/main/resources/application.yml`
   - `mcp-server/src/main/kotlin` — search for `System.getenv` calls or env var references
   - `docker/caddy/Caddyfile` — env vars used by the reverse proxy (`{$VAR}` syntax)

2. **Compare with docs** — read the current env var tables in:
   - `docs/deploy.md` (GitHub Secrets per environment section)
   - `draftsim-parser/CLAUDE.md`
   - `wizard-stat-aggregator/CLAUDE.md`
   - `collection-manager/CLAUDE.md`
   - `auth-service/CLAUDE.md`
   - `mcp-server/CLAUDE.md`

3. **For each module**, identify:
   - Variables present in `application.yml` but missing from the module's `CLAUDE.md` table
   - Variables present in `docs/deploy.md` secrets section but missing from `application.yml` (may be stale)
   - Variables whose default values have changed

4. **Update the tables** in each file:
   - Add missing rows with name, default value (from yml), required flag, and purpose
   - Flag stale rows with a `<!-- stale? -->` comment (do NOT delete without user confirmation)
   - Update changed defaults

5. **Update `docs/deploy.md`** GitHub Secrets sections — add any new secrets required in each `production-<module>` and `production-infra` environment that aren't already listed.

6. **Scan and update scripts** — check all files in `scripts/` and `.github/workflows/` for references that may be outdated:
   - `scripts/deploy.py` — `MODULES` list must contain every deployable unit (all Spring modules + `mcp-server` + `auth-service` + `caddy`). Add any missing entries.
   - `.github/workflows/` — verify a `deploy-<module>.yml` exists for every module in `MODULES`. Note any missing workflows.
   - `.github/workflows/_deploy-module.yml` — check `modules_without_migrations` default covers all non-Spring modules.
   - `docker/docker-compose.prod.yml` — verify every module in `MODULES` has a corresponding service, and that each service references the correct `env_file` (`.infra.env` for caddy, `.<module>.env` for everything else).

7. **Report** a diff summary: what was added, what was changed, what looks potentially stale, and which scripts were updated.
