# wizard-stat-aggregator — CLAUDE.md

Spring Boot service that aggregates limited format statistics from 17lands.com and stores them locally.

## Module Purpose

1. **Fetch**: Pull card limited stats (win rates, pick rates, etc.) from 17lands API for tracked MTG sets
2. **Aggregate**: Store stats per card per format in DB
3. **Serve**: Expose stats via REST API for consumption by MCP server and other clients
4. **Schedule**: Periodic auto-refresh via configurable cron (`SCHEDULER_CARD_LIMITED_STATS_CRON`)

## Key Architecture

### Layering

Same hexagonal pattern as other Spring modules:
- `application/rest` — controllers
- `application/service` — stat aggregation orchestration
- `domain` — `CardLimitedStats`, `WizardStatRepository` port
- `infrastructure/client` — 17lands HTTP client with retry logic
- `infrastructure/db` — Exposed tables + repositories

### HTTP Client (17lands)

Configurable retry with exponential backoff:
- `HTTP_CLIENT_LANDS17_RETRY_MAX_ATTEMPTS` (default: 3)
- `HTTP_CLIENT_LANDS17_RETRY_INITIAL_DELAY_MS` (default: 500)
- `HTTP_CLIENT_LANDS17_RETRY_MULTIPLIER` (default: 2.0)
- `HTTP_CLIENT_LANDS17_RETRY_MAX_DELAY_MS` (default: 5000)

### Scheduler

`SCHEDULER_CARD_LIMITED_STATS_ENABLED=true` activates a Spring `@Scheduled` job.  
The scheduler reads active MTG set codes from `tracked_limited_stat_sets`; rows are active while
`watch_until` is today or in the future.

## Environment Variables

| Variable | Default | Required | Purpose |
|----------|---------|----------|---------|
| `DB_HOST` | — | Yes | PostgreSQL hostname |
| `DB_PORT` | — | Yes | PostgreSQL port |
| `DB_NAME` | — | Yes | Database name (`wizard_stat_db`) |
| `DB_USERNAME` | — | Yes | PostgreSQL user |
| `DB_PASSWORD` | — | Yes | PostgreSQL password |
| `AUTH_ISSUER_URI` | — | Yes | Public URL of auth-service for JWKS validation |
| `LANDS17_BASE_URL` | `https://www.17lands.com` | No | 17lands API base URL |
| `HTTP_CLIENT_LANDS17_RETRY_MAX_ATTEMPTS` | `3` | No | Retry attempts |
| `HTTP_CLIENT_LANDS17_RETRY_INITIAL_DELAY_MS` | `500` | No | Initial retry delay (ms) |
| `HTTP_CLIENT_LANDS17_RETRY_MULTIPLIER` | `2.0` | No | Backoff multiplier |
| `HTTP_CLIENT_LANDS17_RETRY_MAX_DELAY_MS` | `5000` | No | Max retry delay (ms) |
| `SCHEDULER_CARD_LIMITED_STATS_ENABLED` | `true` | No | Enable scheduled aggregation |
| `SCHEDULER_CARD_LIMITED_STATS_CRON` | `@daily` | No | Cron expression |

## Authentication

All REST endpoints (except `/actuator/health`, `/swagger-ui/**`, `/api-docs/**`) require a valid admin JWT:

```
Authorization: Bearer <access_token>
```

The token is validated against JWKS lazily fetched from `AUTH_ISSUER_URI` on first request.
API endpoints require `ROLE_ADMIN`; selected actions also require `PERM_*` authorities from the
JWT `permissions` claim.
Obtain an access token from auth-service `POST /api/v1/auth/login` (see `auth-service/src/test/LOCAL_LOGIN.md`).

**Swagger UI**: use the `Authorize` button (`/swagger-ui.html`) to paste the access token.

**Security tests**: `SecuritySmokeTest` uses `SecurityMockMvcRequestPostProcessors.jwt()` to verify admin and permission guards.

## Database

`wizard_stat_db` — created by `docker/postgres/init-databases.sh` on first PostgreSQL start.

Migrations: `src/main/resources/db/changelog/migrations/`

```bash
./gradlew :wizard-stat-aggregator:createMigration -PsqlName=add_new_table
./gradlew :wizard-stat-aggregator:update
```

## Integration Tests

Extend `AbstractIntegrationTest` (Testcontainers `postgres:16-alpine`). Required for SQL Mappers.
Services and controllers use unit tests with mocked dependencies.
