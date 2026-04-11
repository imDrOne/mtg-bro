# draftsim-parser — CLAUDE.md

Spring Boot service that parses Draftsim articles using Claude (Anthropic) LLM for analysis and insights.

## Module Purpose

1. **Scrape**: Fetch article listings and content from `draftsim.com`
2. **Analyse**: Send article text to Claude Haiku via Anthropic SDK → structured analysis
3. **Publish**: Store analysis results in DB; optionally auto-publish (`ANALYSIS_AUTO_PUBLISH`)

## Key Architecture

### LLM abstraction

`LlmClient` is a domain port. Two implementations:
- `AnthropicLlmClient` — calls Claude Haiku via Anthropic SDK. Active when `infrastructure.llm.client=CLAUDE` (production default).
- `MockLlmClient` — returns `null`, used for local testing without API key. Active when `infrastructure.llm.client=MOCK`.

`AnthropicLlmConfiguration` creates the `AnthropicOkHttpClient` bean; it only loads when `client=CLAUDE`.

### Layering

Same hexagonal pattern as other Spring modules:
- `application/rest` — controllers
- `application/service` — `ArticleAnalysisService` orchestrates scrape → analyse → persist
- `domain` — `Article`, `ArticleAnalysis`, `LlmClient` port
- `infrastructure/client/anthropic` — Anthropic SDK wiring
- `infrastructure/client/draftsim` — HTTP scraper for draftsim.com
- `infrastructure/db` — Exposed tables + repositories

## Environment Variables

| Variable | Default | Required | Purpose |
|----------|---------|----------|---------|
| `DB_HOST` | — | Yes | PostgreSQL hostname |
| `DB_PORT` | — | Yes | PostgreSQL port |
| `DB_NAME` | — | Yes | Database name (`draftsim_parser_db`) |
| `DB_USERNAME` | — | Yes | PostgreSQL user |
| `DB_PASSWORD` | — | Yes | PostgreSQL password |
| `DRAFTSIM_BASE_URL` | `https://draftsim.com` | No | Draftsim website base URL |
| `LLM_CLIENT` | `CLAUDE` | No | `CLAUDE` or `MOCK` |
| `ANTHROPIC_API_KEY` | — | Yes (if `CLAUDE`) | Anthropic API key |
| `ANALYSIS_AUTO_PUBLISH` | `false` | No | Auto-publish analysis results |

## Database

`draftsim_parser_db` — created by `docker/postgres/init-databases.sh` on first PostgreSQL start.

Migrations: `src/main/resources/db/changelog/migrations/`

```bash
./gradlew :draftsim-parser:createMigration -PsqlName=add_new_table
./gradlew :draftsim-parser:update
```

## Integration Tests

Extend `AbstractIntegrationTest` (Testcontainers `postgres:16-alpine`). Required for SQL Mappers.
Services and controllers use unit tests with mocked dependencies.
