# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

mtg-bro is a Magic: The Gathering deck-building assistant consisting of three Kotlin modules:

- **collection-manager**: Spring Boot service for managing card collections, integrates with Scryfall API
- **wizard-stat-aggregator**: Spring Boot service for aggregating limited format statistics from 17lands
- **mcp-server**: MCP (Model Context Protocol) server exposing tools for Claude to search cards and assist with deck building

## Build Commands

```bash
# Build all modules
./gradlew build

# Build Docker images for all services
./gradlew jibDockerBuild

# Run everything locally (postgres + collection-manager + mcp-server + ngrok)
./gradlew runLocal

# Build MCP server for Cursor integration
./gradlew :mcp-server:installDist
```

## Testing

```bash
# Run all tests
./gradlew test

# Run tests for a specific module
./gradlew :collection-manager:test
./gradlew :wizard-stat-aggregator:test

# Run a single test class
./gradlew :collection-manager:test --tests "xyz.candycrawler.collectionmanager.application.parser.MoxfieldFileParserTest"
```

Integration tests use Testcontainers with PostgreSQL (`AbstractIntegrationTest` base class).

## Database Migrations

Uses Liquibase with SQL-based migrations. Each Spring module has its own database.

```bash
# Create a new migration
./gradlew :collection-manager:createMigration -PsqlName=add_new_table
./gradlew :wizard-stat-aggregator:createMigration -PsqlName=add_new_table

# Run migrations (requires local postgres or db.properties configured)
./gradlew :collection-manager:update
./gradlew :wizard-stat-aggregator:update
```

Migration files are in `<module>/src/main/resources/db/changelog/migrations/`.

## Architecture

### Convention Plugins (buildSrc)

- `spring-module`: Kotlin + Spring Boot + Exposed (database) + test dependencies
- `liquibase-module`: Liquibase config + `createMigration` task
- `jib-module`: Docker image building via JIB

### Module Structure

Each Spring module follows hexagonal architecture:
- `application/rest`: Controllers and DTOs
- `application/service`: Application services
- `domain`: Domain models, repositories (interfaces), exceptions
- `infrastructure/db`: Exposed tables, entity records, repository implementations
- `infrastructure/client`: External API clients (Scryfall, 17lands)

### MCP Server

Non-Spring module using Ktor + MCP SDK. Tool handlers are in `mcp-server/src/main/kotlin/.../tools/`. Add new tools by:
1. Creating schema function and handler in a new `*Handler.kt` file
2. Registering with `server.addTool()` in `McpServer.kt`

## Local Development

Start infrastructure:
```bash
docker compose -f docker/docker-compose.local.yml up postgres -d
```

Run Spring services with profile `local`:
```bash
./gradlew :collection-manager:bootRun
./gradlew :wizard-stat-aggregator:bootRun
```

Database credentials for local dev are in `<module>/db.properties`.

# Architecture rules

## Domain layer

- Every domain object is a **Domain Entity** with self-validation.  
  Validation happens in the constructor via `assert` (or `require`):
```kotlin
  class Cat(val name: String, val age: Int) {
      init {
          assert(name.isNotBlank()) { "Cat name must not be blank" }
          assert(age >= 0) { "Cat age must be non-negative" }
      }
  }
```
- Each domain object MUST have its own typed exceptions:
    - `{Entity}NotFoundException` — entity not found in the database
    - `{Entity}InvalidException` — business rule violation  
      Examples: `CatNotFoundException`, `CatInvalidException`, `DogNotFoundException`.
- Domain Entity is the only type allowed to cross layer boundaries.

---

## SQL Builder / Mapper layer

- A dedicated **SQL Builder** (or Mapper) is created for each table,
  adapted to the library in use: Exposed (Kotlin), MyBatis, raw SQL, etc.
- The SQL Builder executes queries and returns a **DB Entity** —
  a class that mirrors the table DDL exactly (1:1 column mapping).
- DB Entity is used ONLY inside the SQL Builder / Mapper. It never escapes this layer.
- The mapping from DB Entity → Domain Entity happens inside the Repository.

---

## Repository layer (CQRS)

### Command Repository
- Naming: `{Entity}Repository` (e.g. `CatRepository`).
- Accepts and returns ONLY Domain Entities.
- Uses the SQL Builder internally; maps DB Entity → Domain Entity before returning.
- Update pattern — functional `update`:
```kotlin
  fun update(id: UUID, block: (Cat) -> Cat): Cat {
      val existing = find(id) ?: throw CatNotFoundException(id)
      val updated = block(existing)
      // persist updated...
      return updated
  }
```
The last argument is a lambda `(fromDb: T) -> T` containing the update logic.  
Under the hood, `find()` is always called first to verify the record exists.

### Query Repository (read-only, CQRS)
- Naming: `Query{Entity}Repository` (e.g. `QueryCatRepository`).
- Read-only. Must not perform any mutations.
- May return either a Domain Entity or a DTO —
  when additional fields are needed that are not present in the domain model (JOINs, aggregates, etc.).
- DTOs do not belong to the Domain layer; they live in the Application / API layer.

---

## Testing strategy

| Component                  | Test type                                     |
|----------------------------|-----------------------------------------------|
| SQL Builder / Mapper       | Integration (real database)                   |
| RMQ / Kafka handler        | Integration (real or embedded broker)         |
| RMQ / Kafka producer       | Integration                                   |
| Domain Entity              | Unit                                          |
| Repository (mapping logic) | Unit (mock SQL Builder)                       |
| Use Case / Service         | Unit                                          |
| Query Repository           | Unit (mock) + integration for complex queries |

- Integration tests use real infrastructure (Testcontainers or embedded).
- Unit tests do not spin up containers and make no network calls.

---

## Summary rules

1. DB Entity never leaves the SQL Builder / Mapper layer.
2. Repository always maps DB → Domain before returning.
3. For updates — always use the functional pattern with `find()` under the hood.
4. Read-only repositories must have the `Query` prefix.
5. Every domain object has its own `NotFoundException` and `InvalidException`.
6. SQL Builders and MQ handlers/producers require integration tests; everything else requires unit tests.
