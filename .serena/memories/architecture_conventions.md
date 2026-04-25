# Architecture & Conventions

## Spring Modules — Hexagonal Architecture
```
application/rest      — Controllers, DTOs
application/service   — Application services (use cases)
domain/               — Domain entities, repository interfaces, exceptions
infrastructure/db     — Exposed tables, DB entities, repository impls
infrastructure/client — External API clients (Scryfall, 17lands, Anthropic)
```

## Domain Layer Rules
- Domain entity = self-validating via `init { require(...) }` or `assert`
- Every entity needs: `{Entity}NotFoundException`, `{Entity}InvalidException`
- Domain Entity is the ONLY type crossing layer boundaries

## DB / Repository Layer
- SQL Builder executes queries → returns DB Entity (1:1 table mirror)
- DB Entity NEVER leaves SQL Builder layer
- Repository maps DB Entity → Domain Entity before returning
- Command repo: `{Entity}Repository` — accepts/returns Domain Entities
- Query repo (read-only): `Query{Entity}Repository` — may return DTOs for JOINs
- Update pattern: `fun update(id, block: (Entity) -> Entity)` — always fetches first

## MCP Server Architecture
- Each tool = `*Handler.kt` with `*Schema()` + `handle*()` functions
- Tools registered in `McpServer.kt` via `server.addTool()`
- `ToolContext` = shared context (baseUrl, httpClient)
- `FilteredMcpServer` = RBAC wrapper (YAML-configurable tool access per role)
- Auth: `McpAuthPlugin.kt` (JWT validation), `OAuthMetadataRoute.kt` (RFC 9728)
- Transports: `stdio` (Cursor, no auth) | `http` (Claude web, OAuth 2.1)

## Convention Plugins (buildSrc)
- `spring-module` — Kotlin + Spring Boot + Exposed + test deps
- `liquibase-module` — Liquibase + `createMigration` task
- `jib-module` — Docker multi-platform (amd64 + arm64)

## Tech Stack
- Kotlin + Java 21
- Spring Boot (web, jdbc, logging)
- Jetbrains Exposed ORM
- Ktor (mcp-server only)
- PostgreSQL
- Testcontainers (integration tests)
- Mockito-Kotlin (unit tests)
- Liquibase (migrations, SQL-based)
- JIB (Docker builds)

## Naming Conventions
- Kotlin idiomatic (camelCase, PascalCase for classes)
- No hungarian notation
- Strict JSR-305 null-safety (`-Xjsr305=strict`)
- `-Xannotation-default-target=param-property`
