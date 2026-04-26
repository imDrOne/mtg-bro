# auth-service

Spring Boot сервис аутентификации и авторизации на базе Spring Authorization Server (OAuth 2.0 + OIDC).

**Port:** 8080 (внутри контейнера), маппится на 8083 на хосте.  
**DB:** `auth_service_db`  
**Package root:** `xyz.candycrawler.authservice`

---

## Полный флоу аутентификации

### 1. Регистрация пользователя

```
POST /api/v1/users/register
Content-Type: application/json

{ "email": "user@example.com", "username": "user", "password": "secret123" }

→ 201 Created
{ "id": 1, "email": "user@example.com", "username": "user", "createdAt": "..." }
```

Реализация: `UserRegistrationController` → `UserRegistrationService` → `JdbcUserRepository`.  
Пароль хешируется через `BCryptPasswordEncoder`. Дубликат email/username → 422.  
Rate limit: 5 запросов / 10 минут на IP.

---

### 2. OAuth 2.0 Authorization Code + PKCE (для MCP-клиентов)

Это основной флоу, по которому Claude.ai (или другой MCP-клиент) получает доступ к данным пользователя.

```
MCP-клиент                    auth-service
     │                              │
     │─── GET /oauth2/authorize ───►│
     │    ?client_id=mcp-client     │
     │    &redirect_uri=...         │
     │    &scope=openid+decks:read  │
     │    &code_challenge=...       │
     │    &code_challenge_method=S256
     │                              │
     │                              │── redirect /login
     │                              │   (login-страница отдаётся auth-service)
     │                              │
     │                              │◄─── POST /login
     │                              │     email + password
     │                              │
     │                              │─── redirect back to
     │                              │    /oauth2/authorize
     │                              │
     │◄─── redirect redirect_uri ───│
     │     ?code=<auth_code>        │
     │                              │
     │─── POST /oauth2/token ──────►│
     │    grant_type=authorization_code
     │    code=<auth_code>          │
     │    code_verifier=...         │
     │                              │
     │◄─── access_token + id_token ─│
     │     + refresh_token          │
```

> **Phase 1 (текущая):** используется дефолтная страница логина Spring Security (`DefaultLoginPageGeneratingFilter`).
> Кастомный `loginPage` не задан — Spring генерирует форму на `/login` автоматически.
> Phase 2 — кастомные Thymeleaf-шаблоны внутри auth-service (см. раздел ниже).

**Ключевые параметры MCP-клиента** (`oauth2_registered_client`):
- `client_id`: `mcp-client`
- `grant_types`: `authorization_code, refresh_token`
- `scopes`: `openid, profile, decks:read`
- `requireProofKey`: `true` (PKCE обязателен)
- `requireAuthorizationConsent`: `false`
- `redirect_uri`: `https://claude.ai/api/mcp/auth_callback`

**OIDC Discovery:** `GET /.well-known/openid-configuration`  
**JWKS:** `GET /oauth2/jwks`  
**Token:** `POST /oauth2/token`  
**Revoke:** `POST /oauth2/revoke` (требует client credentials)  
**Logout:** `GET /connect/logout` (OIDC RP-Initiated Logout)

---

### 3. Обычный логин (form login для браузерного флоу)

Spring Security обрабатывает `POST /login` с полями `username` (= email) и `password`.

**Успех:** Spring перенаправляет обратно на URL, с которого пришёл (обычно `/oauth2/authorize`).  
**Ошибка:** `jsonAuthenticationFailureHandler` возвращает `HTTP 401`:
```json
{ "error": "Invalid credentials" }
```

Одинаковый ответ при неверном пароле и при несуществующем пользователе — намеренно, чтобы не раскрывать факт существования аккаунта. `hideUserNotFoundExceptions=true` (дефолт Spring).

Rate limit: 10 запросов / 5 минут на IP.

---

### 4. RSA ключи (JWT signing)

При старте `AuthorizationServerConfig.jwkSource()` вызывает `RsaKeySqlMapper.loadOrGenerate()`:
1. SELECT из таблицы `rsa_keys` (ограничена одной строкой через CHECK constraint).
2. Если нашёл — десериализует RSA ключ из Base64 DER.
3. Если не нашёл — генерирует RSA 2048, сохраняет в транзакции, возвращает.

Это гарантирует, что JWT-токены остаются валидными после рестарта сервиса.

---

### Ручное тестирование OAuth2 flow

Пошаговая инструкция с curl-командами для проверки полного Authorization Code + PKCE flow:
[`src/test/MANUAL_TESTING.md`](src/test/MANUAL_TESTING.md)

---

### 3. Cookie-based login flow (для собственного UI)

Параллельный флоу — не требует PKCE и browser-redirect. Используется напрямую фронтендом или при ручном тестировании.

#### Endpoints

```
POST /api/v1/auth/login
Content-Type: application/json

{ "email": "user@example.com", "password": "secret123" }

→ 200 OK
{ "accessToken": "<jwt>" }
Set-Cookie: refresh_token=<hash>; HttpOnly; Path=/api/v1/auth; SameSite=Strict; Secure
```

```
POST /api/v1/auth/refresh      # Cookie: refresh_token=<hash> (из предыдущего ответа)

→ 200 OK
{ "accessToken": "<new-jwt>" }
Set-Cookie: refresh_token=<new-hash>; HttpOnly; ...
```

```
POST /api/v1/auth/logout       # Cookie: refresh_token=<hash>

→ 204 No Content
Set-Cookie: refresh_token=; Max-Age=0   # cookie очищен
```

**Refresh-token rotation:** каждый вызов `/refresh` выдаёт новый токен и ревокит старый.  
**Reuse detection:** повторное использование уже ревокнутого токена → все токены пользователя ревокнутся, ответ 401.  
**Logout идемпотентен:** вызов без cookie или с неизвестным токеном возвращает 204 без ошибки.

Флаг `Secure` на cookie управляется env-переменной `AUTH_REFRESH_COOKIE_SECURE` (default `true`; для локальной разработки — `false`, см. ниже).

Ручной тест через curl: [`src/test/LOCAL_LOGIN.md`](src/test/LOCAL_LOGIN.md)

---

## Кастомизация UI (Thymeleaf) — Phase 2

Login-страница **живёт внутри auth-service** — это стандартная модель Identity Provider (Keycloak, Auth0, Okta). Браузер всегда отправляет credentials на домен auth-service, что гарантирует корректную работу session cookie без cross-domain проблем.

> **Сейчас (Phase 1):** используется дефолтная страница Spring Security (`/login`).  
> **Phase 2:** кастомные Thymeleaf-шаблоны внутри auth-service.

### Шаг 1 — Добавить Thymeleaf в зависимости

```kotlin
// auth-service/build.gradle.kts
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
}
```

### Шаг 2 — Создать шаблоны

```
auth-service/src/main/resources/templates/
    login.html      # форма логина
    register.html   # форма регистрации (опционально)
```

### Шаг 3 — Настроить loginPage в SecurityConfig

Когда `templates/login.html` создан, явно задать путь в `SecurityConfig`:

```kotlin
.formLogin { form ->
    form
        .loginPage("/login")          // GET /login → Spring рендерит login.html
        .loginProcessingUrl("/login") // POST /login → Spring обрабатывает credentials
        .failureUrl("/login?error")   // ошибка → редирект с ?error параметром
        .permitAll()
}
```

И обновить `LoginUrlAuthenticationEntryPoint` в `AuthorizationServerConfig`:

```kotlin
LoginUrlAuthenticationEntryPoint("/login")
```

> При переходе с Phase 1 на Phase 2 убрать `failureHandler(jsonAuthenticationFailureHandler())` —
> он переопределяет поведение ошибки и мешает `failureUrl` работать для браузерного редиректа.

### Шаг 4 — Пример шаблона login.html

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head><title>Login</title></head>
<body>
  <form method="post" th:action="@{/login}">
    <input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}" />
    <input type="email"    name="username" placeholder="Email" required />
    <input type="password" name="password" placeholder="Password" required />
    <button type="submit">Sign in</button>
    <div th:if="${param.error}">Invalid email or password</div>
  </form>
  <a th:href="@{/register}">Create account</a>
</body>
</html>
```

> Поле логина называется `username` (Spring convention), даже если в нём вводится email.  
> CSRF-токен обязателен — `/login` не отключён в `csrf.ignoringRequestMatchers`.

### Шаг 5 — Logout

Клиентское приложение инициирует OIDC logout:

```
GET https://auth.yourdomain.com/connect/logout
    ?id_token_hint=<id_token>
    &post_logout_redirect_uri=https://client.yourdomain.com
    &state=<random>
```

Spring AS инвалидирует сессию и редиректит на `post_logout_redirect_uri`.

Для этого URI должен быть зарегистрирован в `oauth2_registered_client.post_logout_redirect_uris`.
Добавить в seed-миграцию:
```sql
post_logout_redirect_uris = 'https://claude.ai'
```

---

## JWT Claims

Access tokens issued by `AccessTokenIssuer` contain the following custom claims:

| Claim | Type | Description |
|---|---|---|
| `permissions` | `List<String>` | Granted permission scopes (e.g. `PERM_api:decks:read`) |
| `roles` | `List<String>` | User roles (e.g. `ROLE_USER`) |
| `user_id` | `Long` (BIGINT) | Numeric user ID from `auth_service_db.users.id`. Used by downstream services (e.g. collection-manager) to scope data per user. |

> **Not** the JWT `sub` (which is the email). Downstream services must read `user_id` claim, not `sub`.

---

## Конфигурация окружения

| Переменная | Описание | Пример (prod) |
|---|---|---|
| `DB_HOST` | PostgreSQL хост | `postgres` |
| `DB_PORT` | PostgreSQL порт | `5432` |
| `DB_NAME` | Имя базы | `auth_service_db` |
| `DB_USERNAME` | Пользователь БД | `wizard_user` |
| `DB_PASSWORD` | Пароль БД | _(secret)_ |
| `AUTH_ISSUER_URI` | Публичный URL auth-service | `https://auth.yourdomain.com` |
| `MCP_CLIENT_REDIRECT_URI` | Redirect URI MCP-клиента | `https://claude.ai/api/mcp/auth_callback` |
| `AUTH_TRUSTED_PROXY_CIDR` | CIDR доверенного прокси | `172.16.0.0/12` (Docker default) |
| `AUTH_REFRESH_COOKIE_SECURE` | Флаг `Secure` на refresh-cookie | `true` (prod); `false` для local-профиля / `docker-compose.local` |

Локальная разработка — `application-local.yml`, база на `localhost:5432/auth_service_db`.

---

## Rate Limiting

| Endpoint | Лимит |
|---|---|
| `POST /api/v1/users/register` | 5 запросов / 10 минут на IP |
| `POST /login` | 10 запросов / 5 минут на IP |
| `POST /api/v1/auth/login` | TODO — лимит пока не реализован |
| `POST /api/v1/auth/refresh` | TODO — лимит пока не реализован |

Реализован через `Bucket4jCaffeine` (per-instance, in-memory). `X-Forwarded-For` принимается только от доверенного прокси (`AUTH_TRUSTED_PROXY_CIDR`).

---

## Ручное создание BCrypt-хеша для MCP клиента

```bash
# Python (нужен pip install bcrypt):
python3 -c "import bcrypt; print(bcrypt.hashpw(b'your-secret', bcrypt.gensalt(10)).decode())"

# Kotlin REPL:
kotlin -e "import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder; println(BCryptPasswordEncoder().encode(\"your-secret\"))"
```

Вставить результат в миграцию **без** `{bcrypt}` префикса — `PasswordEncoder` bean = `BCryptPasswordEncoder`,
который принимает чистый bcrypt-хеш напрямую:
```sql
client_secret = '$2a$10$...'
```
