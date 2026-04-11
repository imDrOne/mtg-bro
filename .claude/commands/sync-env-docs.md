Scan all module `application.yml` files and sync the environment variable tables in `docs/deploy.md` and each module's `CLAUDE.md`.

## Steps

1. **Read current env vars from source of truth** for each of the four modules:
   - `collection-manager/src/main/resources/application.yml`
   - `draftsim-parser/src/main/resources/application.yml`
   - `wizard-stat-aggregator/src/main/resources/application.yml`
   - `mcp-server/src/main/kotlin` — search for `System.getenv` calls or env var references

2. **Compare with docs** — read the current env var tables in:
   - `docs/deploy.md` (GitHub Secrets per environment section)
   - `draftsim-parser/CLAUDE.md`
   - `wizard-stat-aggregator/CLAUDE.md`
   - `collection-manager/CLAUDE.md`
   - `mcp-server/CLAUDE.md`

3. **For each module**, identify:
   - Variables present in `application.yml` but missing from the module's `CLAUDE.md` table
   - Variables present in `docs/deploy.md` secrets section but missing from `application.yml` (may be stale)
   - Variables whose default values have changed

4. **Update the tables** in each file:
   - Add missing rows with name, default value (from yml), required flag, and purpose
   - Flag stale rows with a `<!-- stale? -->` comment (do NOT delete without user confirmation)
   - Update changed defaults

5. **Update `docs/deploy.md`** GitHub Secrets sections — add any new secrets required in each `production-<module>` environment that aren't already listed.

6. **Report** a diff summary: what was added, what was changed, what looks potentially stale.
