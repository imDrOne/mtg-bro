# draftsim-parser — CLAUDE.md

Spring Boot service that parses Draftsim articles using Spring AI for article analysis, embeddings, and semantic article search.

## Module Purpose

1. **Scrape**: Fetch article listings and content from `draftsim.com`
2. **Analyse**: Send article text through Spring AI chat → structured analysis
3. **Index**: Store analysis embeddings in Qdrant for semantic search
4. **Publish**: Store analysis results in DB; optionally auto-publish (`ANALYSIS_AUTO_PUBLISH`)

## Key Architecture

### AI abstraction

`LlmClient` is a domain port. Two implementations:
- `SpringAiLlmClient` — calls the configured Spring AI chat provider. Active when `infrastructure.llm.client=SPRING_AI` (production default).
- `MockLlmClient` — returns `null`, used for local testing without API key. Active when `infrastructure.llm.client=MOCK`.

Provider and model selection are environment-driven:
- `AI_CHAT_PROVIDER` / `AI_CHAT_MODEL` for article analysis
- `AI_EMBEDDING_PROVIDER` / `AI_EMBEDDING_MODEL` for vector indexing

Qdrant access is intentionally kept inside this service for now. It is wrapped behind the `ArticleVectorStore` port so it can later move to a separate vector service if multiple writers or shared schema ownership become real requirements.

### Layering

Same hexagonal pattern as other Spring modules:
- `application/rest` — controllers
- `application/service` — `ArticleAnalysisService` orchestrates scrape → analyse → persist
- `domain` — `Article`, `ArticleAnalysis`, `LlmClient` port
- `infrastructure/client/springai` — Spring AI chat wiring
- `infrastructure/vector` — Spring AI vector store adapter for Qdrant
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
| `AUTH_ISSUER_URI` | — | Yes | Public URL of auth-service for JWKS validation |
| `DRAFTSIM_BASE_URL` | `https://draftsim.com` | No | Draftsim website base URL |
| `LLM_CLIENT` | `SPRING_AI` | No | `SPRING_AI` or `MOCK` |
| `AI_CHAT_PROVIDER` | `anthropic` | No | Spring AI chat provider |
| `AI_CHAT_MODEL` | `claude-haiku-4-5-20251001` | No | Chat model for article analysis |
| `ANTHROPIC_API_KEY` | — | Yes (if Anthropic chat) | Anthropic API key |
| `AI_EMBEDDING_PROVIDER` | `openai` | No | Spring AI embedding provider |
| `AI_EMBEDDING_MODEL` | `text-embedding-3-small` | No | Embedding model |
| `OPENAI_API_KEY` | — | Yes (if OpenAI embeddings) | OpenAI API key |
| `QDRANT_HOST` | `localhost` | No | Qdrant host |
| `QDRANT_PORT` | `6334` | No | Qdrant gRPC port |
| `QDRANT_API_KEY` | — | No | Qdrant API key if enabled |
| `QDRANT_COLLECTION` | `draftsim_article_insights` | No | Vector collection for article insights |
| `QDRANT_INITIALIZE_SCHEMA` | `false` | No | Whether Spring AI should initialize vector schema |
| `ARTICLE_VECTOR_INDEX_ENABLED` | `true` | No | Enable Qdrant indexing and semantic search |
| `ARTICLE_VECTOR_SEARCH_CACHE_MAX_SIZE` | `500` | No | Max cached semantic search result sets |
| `ARTICLE_VECTOR_SEARCH_CACHE_TTL` | `PT10M` | No | Semantic search cache TTL |
| `ANALYSIS_AUTO_PUBLISH` | `false` | No | Auto-publish analysis results |

## Authentication

All REST endpoints (except `/actuator/health`, `/swagger-ui/**`, `/v3/api-docs/**`) require a valid JWT:

```
Authorization: Bearer <access_token>
```

The token is validated against JWKS lazily fetched from `AUTH_ISSUER_URI` on first request.
Obtain an access token from auth-service `POST /api/v1/auth/login` (see `auth-service/src/test/LOCAL_LOGIN.md`).

**Swagger UI**: use the `Authorize` button (`/swagger-ui.html`) to paste the access token.

**Security tests**: no `SecuritySmokeTest` exists yet — test infrastructure requires `ANTHROPIC_API_KEY` which is not available in CI without secrets.

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
