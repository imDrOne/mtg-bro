# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Module Purpose

Spring Boot service that manages a local MTG card collection. Core flows:
1. **Import**: Parse TCGPlayer/Moxfield export files → batch-fetch card metadata from Scryfall → upsert cards + collection entries to DB
2. **Search**: Query the local card DB with rich filters; results include per-card owned quantities from the collection
3. **Scryfall Proxy**: Forward card search queries directly to Scryfall API

## Key Architecture

### Layering: SQL Mapper → Repository → Service

The DB layer has three distinct roles:
- **`*SqlMapper`** (`infrastructure/db/mapper/sql/`) — executes Exposed queries and returns `*Record` DB entities. Methods are `internal`. Never accessed outside the repository.
- **`*Record`** (`infrastructure/db/entity/`) — 1:1 mirror of the table DDL. Stays inside the SQL Mapper layer.
- **`Exposed*Repository`** — implements the domain `*Repository` interface. Calls the SQL Mapper and uses `*RecordTo*Mapper` / `*To*RecordMapper` to convert between records and domain entities before returning.

### Coroutines

`CollectionImportService.import` is a `suspend` function. It fans out Scryfall batch calls concurrently with `async/awaitAll` on `Dispatchers.IO`, then switches back to IO to persist. Controllers that call `import` must also be `suspend`.

Scryfall's `/cards/collection` endpoint accepts at most **75 identifiers** per request (`SCRYFALL_BATCH_SIZE = 75`).

### Import pipeline

`CollectionImportController` → `CollectionImportService`:
1. `CollectionFileParser.parse()` — returns `ParsedCollectionEntry` list
2. Merge duplicates by `(setCode, collectorNumber, foil)` key
3. Batch-fetch from Scryfall using `(setCode, collectorNumber)` identifiers
4. `CollectionPersistenceService.saveImportedData()` — single transaction: upsert `Card`s, then upsert `CollectionEntry`s

### Card search

`CardSearchController` calls `cardRepository.search(criteria)` which delegates to `CardSqlMapper.search()`. After fetching the page it joins quantity data from `CollectionEntryRepository.findByCardIds()` in the application layer (not in SQL).

## Collection Invariants

- `collection_entries.quantity` is always > 0. A card's presence in `collection_entries` implies ownership. There are no zero-quantity placeholder rows.

## Adding a new collection file format

1. Implement `CollectionFileParser` — parse lines into `ParsedCollectionEntry`
2. Inject the new parser into `CollectionImportController` and add a `@PostMapping` endpoint similar to the existing ones

## Environment Variables

| Variable | Default | Required | Purpose |
|----------|---------|----------|---------|
| `DB_HOST` | — | Yes | PostgreSQL hostname |
| `DB_PORT` | — | Yes | PostgreSQL port |
| `DB_NAME` | — | Yes | Database name (`collection_manager_db`) |
| `DB_USERNAME` | — | Yes | PostgreSQL user |
| `DB_PASSWORD` | — | Yes | PostgreSQL password |
| `AUTH_ISSUER_URI` | — | Yes | Public URL of auth-service for JWKS validation |
| `SCRYFALL_BASE_URL` | `https://api.scryfall.com` | No | Scryfall API base URL |
| `HTTP_CLIENT_SCRYFALL_RETRY_MAX_ATTEMPTS` | `3` | No | Retry attempts for Scryfall calls |
| `HTTP_CLIENT_SCRYFALL_RETRY_INITIAL_DELAY_MS` | `100` | No | Initial retry delay (ms) |
| `HTTP_CLIENT_SCRYFALL_RETRY_MULTIPLIER` | `2.0` | No | Backoff multiplier |
| `HTTP_CLIENT_SCRYFALL_RETRY_MAX_DELAY_MS` | `2000` | No | Max retry delay (ms) |

## Authentication

All REST endpoints (except `/actuator/health`, `/swagger-ui/**`, `/v3/api-docs/**`) require a valid JWT:

```
Authorization: Bearer <access_token>
```

The token is validated against JWKS lazily fetched from `AUTH_ISSUER_URI` on first request.
Obtain an access token from auth-service `POST /api/v1/auth/login` (see `auth-service/src/test/LOCAL_LOGIN.md`).

**Swagger UI**: use the `Authorize` button (`/swagger-ui.html`) to paste the access token — all subsequent requests will include it.

**Security tests**: `SecuritySmokeTest` uses `SecurityMockMvcRequestPostProcessors.jwt()` to verify protected endpoints return 401 without a token and pass with a mock JWT.

## Integration Tests

Extend `AbstractIntegrationTest` (starts a `postgres:16-alpine` Testcontainer, sets `spring.datasource.*` via `@DynamicPropertySource`). Required for SQL Mappers and Repositories. Services and controllers use unit tests with mocked dependencies.
