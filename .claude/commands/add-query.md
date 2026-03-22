You are helping the user add a new database query to the `collection-manager` module following the project's strict layering rules.

## Step 1 — gather requirements

Ask the user:
1. Which domain entity is this query for? (e.g. `Card`, `CollectionEntry`)
2. What should the query do? (brief description)
3. Does the result need **extra fields** that are NOT present in the domain entity (e.g. from a JOIN or aggregate)? Answer yes/no.

Wait for the answers before proceeding.

---

## Step 2 — decide which pattern to use

### Pattern A — result maps 1:1 to the domain entity

Use when the query returns only columns that already exist on the domain entity.

- Add a new `internal fun` method to the **existing** `{Entity}SqlMapper` (e.g. `CardSqlMapper`)
- The method returns `List<{Entity}Record>` or `{Entity}Record?`
- Add the corresponding method to the **existing** `{Entity}Repository` interface and `Exposed{Entity}Repository` implementation; it maps records → domain entity before returning

### Pattern B — result includes extra fields (JOIN, aggregate, etc.)

Use when the query needs fields not present on the domain entity.

Create these new files:

1. **DTO** in `application/rest/dto/response/` or `application/service/` (wherever it will be consumed) — a plain data class with all required fields including the extras
2. **`{Entity}QuerySqlMapper`** in `infrastructure/db/mapper/sql/` — annotated `@Component`; all methods `internal`; returns a dedicated `{Entity}QueryRecord` (a data class defined in the same file or `infrastructure/db/entity/`)
3. **`Query{Entity}Repository`** interface in `domain/{entity}/repository/` — read-only, returns the DTO or domain entity + extras; no mutations allowed
4. **`Exposed{Entity}QueryRepository`** in `infrastructure/db/repository/` — annotated `@Repository @Transactional(readOnly = true)`; injects `{Entity}QuerySqlMapper`; maps `{Entity}QueryRecord` → DTO before returning

Naming examples for a `Profile` entity:
- `ProfileQuerySqlMapper.kt` / `ProfileQueryRecord`
- `QueryProfileRepository.kt` (interface)
- `ExposedProfileQueryRepository.kt`

---

## Step 3 — implement

After the user confirms the pattern, implement all required files following the conventions above. Read the existing `CardSqlMapper`, `ExposedCardRepository`, and `CardRepository` as style references before writing any code.

Remind the user that:
- `{Entity}QueryRecord` must **never** leave the `{Entity}QuerySqlMapper`
- `Query{Entity}Repository` must contain **no mutating methods**
- Integration tests are required for the new SQL mapper (`AbstractIntegrationTest`)
