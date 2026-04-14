# Kotlin 2.1 → 2.3 + Dependency Upgrade Plan

## Motivation

Ktor 3.3+ and MCP Kotlin SDK 0.10.0+ are compiled with Kotlin 2.3.x.
To use them, the entire monorepo needs a Kotlin compiler upgrade.

## Current Versions (as of April 2026)

| Dependency | Current | Latest | Requires Kotlin |
|---|---|---|---|
| Kotlin | 2.1.21 | **2.3.10** | - |
| Ktor | 3.2.4 | **3.4.1** | 2.3.x |
| MCP Kotlin SDK | 0.9.0 | **0.11.1** | 2.3.x |
| kotlinx-coroutines | 1.10.2 | 1.10.2 | 2.1+ |
| Spring Boot | 4.0.2 | check latest | verify compatibility |

## Scope

Kotlin compiler version lives in `buildSrc/build.gradle.kts`:

```kotlin
implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.21")
implementation("org.jetbrains.kotlin:kotlin-allopen:2.1.21")
```

Changing it affects all 5 modules:
- collection-manager (Spring Boot + Exposed)
- wizard-stat-aggregator (Spring Boot + Exposed)
- draftsim-parser (Spring Boot + Exposed)
- auth-service (Spring Boot + Spring Authorization Server)
- mcp-server (Ktor + MCP SDK)

## Step-by-Step Plan

### 1. Upgrade Kotlin: 2.1.21 → 2.3.10

File: `buildSrc/build.gradle.kts`

```kotlin
implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.10")
implementation("org.jetbrains.kotlin:kotlin-allopen:2.3.10")
```

Check: `./gradlew build` — fix any deprecation-turned-error changes.

Kotlin 2.3 changes to watch for:
- K2 compiler is default (already was in 2.1 for most projects)
- Possible stricter type inference in some edge cases

### 2. Upgrade Ktor: 3.2.4 → 3.4.1 (mcp-server only)

File: `mcp-server/build.gradle.kts`

```kotlin
val ktorVersion = "3.4.1"
```

### 3. Upgrade MCP Kotlin SDK: 0.9.0 → 0.11.1

File: `mcp-server/build.gradle.kts`

```kotlin
val mcpVersion = "0.11.1"
```

Breaking changes in 0.10.0:
- All symbols deprecated at ERROR in 0.9.0 are **removed**
  - File `io.modelcontextprotocol.kotlin.sdk.types.kt` removed
  - Our imports from `io.modelcontextprotocol.kotlin.sdk.types` (package) are NOT affected
- `mcpStreamableHttp()` **auto-installs** `ContentNegotiation` with `McpJson`
  - Remove manual `install(ContentNegotiation) { json(McpJson) }` from `Main.kt`

New features in 0.10.0+:
- SSE reconnection with retry support
- Configurable max request payload size
- URL-mode elicitation

### 4. Verify Spring Boot Compatibility

Spring Boot 4.0.2 should be compatible with Kotlin 2.3.x.
If not, check for a newer Spring Boot version.

Run full test suite: `./gradlew test`

### 5. Verify Exposed Compatibility

Check that the Exposed ORM version used in Spring modules is compatible with Kotlin 2.3.x.

### 6. Full Verification

```bash
./gradlew build                    # all modules compile
./gradlew test                     # all tests pass
./gradlew jibDockerBuild           # Docker images build
./gradlew runLocal                 # local stack starts
```

## Risk Assessment

| Risk | Impact | Mitigation |
|---|---|---|
| Kotlin 2.3 breaks Spring modules | High | Run full test suite, check Spring Boot Kotlin 2.3 compat |
| Exposed ORM incompatibility | Medium | Check Exposed release notes for Kotlin 2.3 support |
| MCP SDK API changes | Low | Only types.kt file removed; our imports unaffected |
| Ktor 3.4 API changes | Low | Minor version, backward compatible |

## Recommendation

Do this upgrade on a separate branch. Run full CI before merging.
Consider upgrading Kotlin first (step 1) in isolation, then Ktor + MCP SDK (steps 2-3).
