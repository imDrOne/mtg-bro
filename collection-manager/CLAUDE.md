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

## Adding a new collection file format

1. Implement `CollectionFileParser` — parse lines into `ParsedCollectionEntry`
2. Inject the new parser into `CollectionImportController` and add a `@PostMapping` endpoint similar to the existing ones

## Integration Tests

Extend `AbstractIntegrationTest` (starts a `postgres:16-alpine` Testcontainer, sets `spring.datasource.*` via `@DynamicPropertySource`). Required for SQL Mappers and Repositories. Services and controllers use unit tests with mocked dependencies.
