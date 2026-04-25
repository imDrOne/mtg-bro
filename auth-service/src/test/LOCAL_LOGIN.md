# Local Login Manual Testing

Start infrastructure:
```bash
docker compose -f docker/docker-compose.local.yml up postgres auth-service -d
```

`AUTH_REFRESH_COOKIE_SECURE=false` is set in the local compose, so refresh cookies work over plain http.

---

## 1. Register a user

```bash
curl -s -X POST http://localhost:8083/api/v1/users/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"me@example.com","username":"testuser","password":"secret123"}' | jq .
```

---

## 2. Login — get access token + refresh cookie

```bash
curl -s -i -c /tmp/auth.jar \
  -X POST http://localhost:8083/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"me@example.com","password":"secret123"}'
```

Copy the `accessToken` from the JSON response body.

---

## 3. Call a protected endpoint

```bash
ACCESS_TOKEN="<paste token here>"

# collection-manager
curl -s -H "Authorization: Bearer $ACCESS_TOKEN" \
  http://localhost:8081/api/v1/cards/search | jq .

# wizard-stat-aggregator
curl -s -H "Authorization: Bearer $ACCESS_TOKEN" \
  http://localhost:8082/api/v1/stats | jq .
```

Alternatively open Swagger UI and click **Authorize**:
- collection-manager: http://localhost:8081/swagger-ui.html
- draftsim-parser: http://localhost:8085/swagger-ui.html
- wizard-stat-aggregator: http://localhost:8082/swagger-ui.html

---

## 4. Refresh

```bash
curl -s -i -b /tmp/auth.jar -c /tmp/auth.jar \
  -X POST http://localhost:8083/api/v1/auth/refresh | jq .
```

Returns a new access token and rotates the refresh cookie.

---

## 5. Replay the old token (reuse detection)

Repeat step 4 again using the **same** cookie jar without refreshing — the second call should return 401 and cascade-revoke all tokens for the user.

---

## 6. Logout

```bash
curl -s -i -b /tmp/auth.jar -c /tmp/auth.jar \
  -X POST http://localhost:8083/api/v1/auth/logout
```

Response: `204 No Content`. Cookie is cleared (`maxAge=0`).

After logout, `/api/v1/auth/refresh` must return 401.
