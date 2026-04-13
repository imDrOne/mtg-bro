# Ручное тестирование OAuth2 flow

Полный Authorization Code + PKCE flow через curl.

## Предварительная настройка

### 1. Запустить postgres + auth-service

```bash
docker compose -f docker/docker-compose.local.yml up postgres auth-service -d
```

### 2. Создать БД и применить миграции (только первый раз)

```bash
# Если БД ещё не создана:
docker exec <postgres-container> psql -U wizard_user -d wizard_stat_db \
  -c "CREATE DATABASE auth_service_db;"

# Применить миграции (нужен auth-service/db.properties):
./gradlew :auth-service:update
```

### 3. Установить client_secret

В seed-миграции стоит `REPLACE_WITH_BCRYPT_HASH`. Нужно сгенерировать BCrypt-хеш
и обновить запись в БД.

Генерация хеша (любой из способов):

```bash
# Python (нужен pip install bcrypt):
python3 -c "import bcrypt; print(bcrypt.hashpw(b'test-secret', bcrypt.gensalt(10)).decode())"

# Kotlin REPL:
kotlin -e "import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder; println(BCryptPasswordEncoder().encode(\"test-secret\"))"
```

Обновить в БД (**без** `{bcrypt}` префикса):

```bash
docker exec <postgres-container> psql -U wizard_user -d auth_service_db -c "
  UPDATE oauth2_registered_client
  SET client_secret = '\$2b\$10\$YOUR_HASH_HERE'
  WHERE client_id = 'mcp-client';"
```

---

## Тестирование полного OAuth2 flow

### Step 1 — Регистрация пользователя

```bash
curl -s -X POST http://localhost:8083/api/v1/users/register \
  -H "Content-Type: application/json" \
  -d '{"email":"demo@example.com","username":"demouser","password":"password123"}'
```

Ожидаемый ответ: `201 Created` с JSON `{ "id": ..., "email": "demo@example.com", ... }`.

### Step 2 — Генерация PKCE-параметров

```bash
CODE_VERIFIER=$(python3 -c "
import secrets, hashlib, base64
verifier = secrets.token_urlsafe(32)
challenge = base64.urlsafe_b64encode(
    hashlib.sha256(verifier.encode()).digest()
).rstrip(b'=').decode()
print(f'{verifier}|{challenge}')
")
export CV=$(echo $CODE_VERIFIER | cut -d'|' -f1)
export CC=$(echo $CODE_VERIFIER | cut -d'|' -f2)
echo "code_verifier:  $CV"
echo "code_challenge: $CC"
```

### Step 3 — Начало авторизации

```bash
curl -s -v -c /tmp/auth-cookies.txt \
  "http://localhost:8083/oauth2/authorize?client_id=mcp-client&redirect_uri=https://claude.ai/api/mcp/auth_callback&response_type=code&scope=openid&state=test-state&code_challenge=$CC&code_challenge_method=S256" \
  2>&1 | grep "< HTTP\|< Location\|< Set-Cookie"
```

Ожидаемый ответ:
```
HTTP/1.1 302
Set-Cookie: JSESSIONID=...; Path=/; HttpOnly
Location: http://localhost:8083/login
```

Spring сохраняет OAuth-запрос в сессии и редиректит на login-страницу.

### Step 4 — Логин

```bash
curl -s -v -b /tmp/auth-cookies.txt -c /tmp/auth-cookies.txt \
  -X POST http://localhost:8083/login \
  -d "username=demo@example.com&password=password123" \
  2>&1 | grep "< HTTP\|< Location"
```

Ожидаемый ответ:
```
HTTP/1.1 302
Location: http://localhost:8083/oauth2/authorize?...&continue
```

Spring аутентифицировал пользователя и редиректит обратно на `/oauth2/authorize`.

### Step 5 — Получение authorization code

```bash
REDIRECT=$(curl -s -v -b /tmp/auth-cookies.txt \
  "http://localhost:8083/oauth2/authorize?client_id=mcp-client&redirect_uri=https://claude.ai/api/mcp/auth_callback&response_type=code&scope=openid&state=test-state&code_challenge=$CC&code_challenge_method=S256&continue" \
  2>&1 | grep "< Location" | sed 's/< Location: //')

echo "Redirect: $REDIRECT"
```

Ожидаемый ответ:
```
Location: https://claude.ai/api/mcp/auth_callback?code=<AUTH_CODE>&state=test-state
```

Извлечь code:
```bash
AUTH_CODE=$(echo "$REDIRECT" | python3 -c "
import sys, urllib.parse
url = sys.stdin.read().strip()
params = urllib.parse.parse_qs(urllib.parse.urlparse(url).query)
print(params.get('code', [''])[0])
")
echo "Auth code: $AUTH_CODE"
```

### Step 6 — Обмен code на токены

```bash
curl -s -X POST http://localhost:8083/oauth2/token \
  -u "mcp-client:test-secret" \
  -d "grant_type=authorization_code&code=$AUTH_CODE&redirect_uri=https://claude.ai/api/mcp/auth_callback&code_verifier=$CV" \
  | python3 -m json.tool
```

Ожидаемый ответ:
```json
{
    "access_token": "eyJraWQ...",
    "refresh_token": "...",
    "id_token": "eyJraWQ...",
    "token_type": "Bearer",
    "scope": "openid",
    "expires_in": 3599
}
```

### Step 7 — Проверка JWT

Декодировать payload access_token:

```bash
echo "<access_token>" | cut -d'.' -f2 | python3 -c "
import sys, base64, json
data = sys.stdin.read().strip()
data += '=' * (4 - len(data) % 4)
print(json.dumps(json.loads(base64.urlsafe_b64decode(data)), indent=2))
"
```

Ожидаемый payload:
```json
{
  "sub": "demo@example.com",
  "aud": "mcp-client",
  "scope": ["openid"],
  "iss": "http://localhost:8083",
  "exp": ...,
  "iat": ...
}
```

---

## Тестирование отдельных эндпоинтов

### OIDC Discovery

```bash
curl -s http://localhost:8083/.well-known/openid-configuration | python3 -m json.tool
```

### JWKS (публичный RSA-ключ)

```bash
curl -s http://localhost:8083/oauth2/jwks | python3 -m json.tool
```

### Login с неверными credentials

```bash
curl -s -X POST http://localhost:8083/login \
  -d "username=nobody@example.com&password=wrong" \
  -w "\nHTTP %{http_code}\n"
```

Ожидаемый ответ: `HTTP 401` + `{"error": "Invalid credentials"}`.

### Регистрация с дубликатом email

```bash
curl -s -X POST http://localhost:8083/api/v1/users/register \
  -H "Content-Type: application/json" \
  -d '{"email":"demo@example.com","username":"other","password":"password123"}' \
  -w "\nHTTP %{http_code}\n"
```

Ожидаемый ответ: `HTTP 422`.

---

## Быстрый тест через браузер

Открыть URL в браузере (code_challenge фиксирован для простоты):

```
http://localhost:8083/oauth2/authorize?client_id=mcp-client&redirect_uri=https://claude.ai/api/mcp/auth_callback&response_type=code&scope=openid&state=test&code_challenge=dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk&code_challenge_method=S256
```

1. Spring покажет дефолтную login-форму
2. Ввести `demo@example.com` / `password123`
3. Браузер редиректит на `https://claude.ai/api/mcp/auth_callback?code=...&state=test`
4. Скопировать `code` из URL и обменять на токены через curl (Step 6)

При использовании фиксированного code_challenge, code_verifier = `dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk`.
