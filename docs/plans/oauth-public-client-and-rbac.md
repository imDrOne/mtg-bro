# OAuth: Public Client + Role-Based Access Control (RBAC)

## Motivation

Текущая модель требует от пользователя вводить `OAuth Client ID` и `OAuth Client Secret` при подключении MCP в Claude Web.
Это неудобно и создаёт проблему ротации секрета — при его смене ломаются все подключения.

Конечная цель — пользователь вводит только URL (`https://mtg-bro-mcp.duckdns.org/mcp`),
логинится через форму, и получает доступ к тулам **в зависимости от своей роли**.

---

## Current State (April 2026)

| Аспект | Реализация |
|--------|------------|
| OAuth Client | `mcp-client`, confidential (`client_secret_basic` + `client_secret_post`) |
| Авторизация | Все тулы доступны всем аутентифицированным пользователям |
| JWT claims | `sub` (user id), `scope` (openid, profile, decks:read) |
| Роли | Нет |
| Регистрация | Open — любой может зарегистрироваться |

---

## Phase 1: Public Client (убираем Client Secret)

### Что делаем

Переводим `mcp-client` из confidential в public. PKCE уже обязателен (`require-proof-key: true`),
поэтому public client безопасен — Authorization Code + PKCE без секрета является стандартной моделью
для браузерных приложений (RFC 8252, OAuth 2.1 draft).

### Изменения

#### auth-service

**Миграция:**
```sql
UPDATE oauth2_registered_client
SET client_secret                 = NULL,
    client_authentication_methods = 'none'
WHERE client_id = 'mcp-client';
```

**client_settings** не меняется — `require-proof-key: true` уже стоит.

#### mcp-server

Без изменений. MCP-сервер валидирует JWT, ему неважно как он был получен.

#### Пользовательский опыт

Claude Web → "Add custom MCP":
- **URL**: `https://mtg-bro-mcp.duckdns.org/mcp`
- **OAuth Client ID**: `mcp-client`
- **OAuth Client Secret**: *(пустое)*

> **Примечание**: зависит от Claude Web UI — некоторые реализации могут не принимать пустой секрет.
> Если Claude требует непустое поле, оставляем confidential client с документированным "публичным" секретом.
> Альтернатива — Dynamic Client Registration (Phase 3).

### Риски

- Claude Web может не поддерживать public clients (потребует secret). Нужно проверить.
- Если другие MCP-клиенты (Cursor, Cody) требуют secret — понадобится отдельный клиент для них.

### Оценка: 1-2 часа

---

## Phase 2: RBAC — Role-Based Access Control

### Что делаем

Добавляем систему ролей для разграничения доступа к MCP-тулам.

### Модель ролей

| Роль | Описание | Доступные тулы |
|------|----------|----------------|
| `FREE` | Дефолтная при регистрации | search-cards, get-card-details |
| `PRO` | Платная подписка / ручное назначение | Все FREE + deck-analysis, draft-helper, collection-stats |
| `ADMIN` | Администратор | Все тулы + управление пользователями |

> Конкретные тулы — примерные, финальный список определяется при реализации.

### Архитектура

```
┌─────────────────────────────────────────────────────────────┐
│ auth-service                                                │
│                                                             │
│  users table          user_roles table                      │
│  ┌──────────┐         ┌──────────────────┐                  │
│  │ id       │────────►│ user_id          │                  │
│  │ email    │         │ role (enum)      │                  │
│  │ password │         │ granted_at       │                  │
│  └──────────┘         └──────────────────┘                  │
│                                                             │
│  JWT claims:                                                │
│  { "sub": "user-123", "roles": ["FREE"], ... }              │
│                                                             │
└─────────────────────────────────────────────────────────────┘
                          │ JWT
                          ▼
┌─────────────────────────────────────────────────────────────┐
│ mcp-server (McpAuthPlugin)                                  │
│                                                             │
│  1. Validate JWT signature (JWKS) ✓ (уже есть)             │
│  2. Extract "roles" claim from JWT                          │
│  3. For each tool call → check role has access              │
│                                                             │
│  ToolAccessConfig:                                          │
│  mapOf(                                                     │
│    "search-cards"    to setOf(FREE, PRO, ADMIN),            │
│    "deck-analysis"   to setOf(PRO, ADMIN),                  │
│    "admin-stats"     to setOf(ADMIN),                       │
│  )                                                          │
└─────────────────────────────────────────────────────────────┘
```

### Изменения

#### auth-service

1. **Миграция** — таблица `user_roles`:
   ```sql
   CREATE TABLE user_roles (
       user_id    BIGINT      NOT NULL REFERENCES users(id),
       role       VARCHAR(50) NOT NULL,
       granted_at TIMESTAMPTZ NOT NULL DEFAULT now(),
       PRIMARY KEY (user_id, role)
   );
   ```

2. **Domain** — `UserRole` enum + `UserRoleRepository`

3. **JWT Customizer** — добавить `roles` claim в access token:
   ```kotlin
   @Bean
   fun jwtCustomizer(userRoleRepository: UserRoleRepository): OAuth2TokenCustomizer<JwtEncodingContext> =
       OAuth2TokenCustomizer { context ->
           if (context.tokenType == OAuth2TokenType.ACCESS_TOKEN) {
               val userId = context.getPrincipal<Authentication>().name
               val roles = userRoleRepository.findByUserId(userId)
               context.claims.claim("roles", roles.map { it.name })
           }
       }
   ```

4. **Регистрация** — при создании пользователя автоматически назначать роль `FREE`.

#### mcp-server

1. **McpAuthPlugin** — после JWT-валидации извлекать `roles` claim и сохранять в call attributes:
   ```kotlin
   val roles = decodedJwt.getClaim("roles").asList(String::class.java)
   call.attributes.put(UserRolesKey, roles)
   ```

2. **Tool Access Control** — проверка роли перед выполнением тула:
   ```kotlin
   // В хендлере тула
   val roles = call.attributes[UserRolesKey]
   if (!toolAccessConfig.hasAccess(toolName, roles)) {
       return ErrorResult("Access denied: requires PRO subscription")
   }
   ```

3. **Конфигурация доступа** — `ToolAccessConfig` data class, загружаемый из env или хардкод.

### Вопрос дизайна: Scopes vs Roles

| Подход | Плюсы | Минусы |
|--------|-------|--------|
| **Scopes** (OAuth standard) | Стандарт, совместимость | Пользователь выбирает scope на consent screen → запутывает |
| **Roles in JWT** (custom claim) | Простой, серверная логика | Нестандартный claim |
| **Roles → Scopes mapping** | Стандарт + серверная логика | Сложнее реализация |

**Рекомендация:** `roles` как custom claim в JWT. Проще, и MCP-клиенты не должны знать о ролях —
это внутренняя логика сервера. Scopes оставить для OAuth-уровня (`openid`, `profile`).

### Оценка: 1-2 дня

---

## Phase 3: Dynamic Client Registration (опционально)

### Зачем

Если Claude Web или другие MCP-клиенты не поддерживают public clients или единый `client_id`,
можно реализовать [RFC 7591 Dynamic Client Registration](https://datatracker.ietf.org/doc/html/rfc7591).

Клиент сам регистрируется на auth-service → получает свой `client_id` / `client_secret` → автоматически,
пользователю вообще не нужно ничего вводить кроме URL.

### Изменения

#### auth-service

1. Включить endpoint `POST /connect/register` в Spring Authorization Server:
   ```kotlin
   .with(configurer) {
       it.oidc { oidc ->
           oidc.clientRegistrationEndpoint(Customizer.withDefaults())
       }
   }
   ```

2. Настроить политику: какие grant types, scopes, redirect URIs разрешены при динамической регистрации.

#### Пользовательский опыт

Claude Web → "Add custom MCP":
- **URL**: `https://mtg-bro-mcp.duckdns.org/mcp`
- *(Всё остальное автоматически)*

### Риски

- Спам-регистрации клиентов → нужен rate limiting и/или одобрение.
- Не все MCP-клиенты поддерживают DCR.
- Усложнение auth-service.

### Оценка: 2-3 дня

---

## Phase 4: Admin Panel (опционально)

Простой UI или CLI для администратора:

- Просмотр пользователей и их ролей
- Назначение/отзыв ролей
- Просмотр зарегистрированных OAuth-клиентов
- Статистика использования тулов

Может быть реализован как:
- REST endpoints в auth-service + curl/httpie
- Минимальный Thymeleaf UI внутри auth-service
- Отдельный фронтенд (overkill для текущего масштаба)

### Оценка: 1-3 дня (зависит от UI)

---

## Roadmap

```
Phase 1 (Public Client)     ──► Убираем неудобство с секретом
          │
Phase 2 (RBAC)              ──► Разграничение доступа по ролям
          │
Phase 3 (DCR)               ──► Автоматическая регистрация клиентов (если нужно)
          │
Phase 4 (Admin Panel)       ──► Управление пользователями и ролями
```

**Рекомендуемый порядок**: Phase 1 → Phase 2 → Phase 4 → Phase 3 (DCR нужен только если public client не сработает).

---

## Зависимости

| Phase | Зависит от |
|-------|-----------|
| Phase 1 | Проверить поведение Claude Web с пустым secret |
| Phase 2 | Phase 1 (опционально, можно делать параллельно) |
| Phase 3 | Phase 2 (роли должны работать до DCR) |
| Phase 4 | Phase 2 (нужны роли для управления) |
