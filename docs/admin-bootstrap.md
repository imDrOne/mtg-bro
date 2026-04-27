# Admin Bootstrap Guide

Как создать первого администратора в системе вручную — до появления admin UI.

---

## Предварительные требования

- Запущен `auth-service` (БД применила миграции)
- Доступ к PostgreSQL контейнеру

---

## Способ 1: Скрипт (рекомендуется)

```bash
bash scripts/create-admin.sh
```

Скрипт запросит email, username и пароль, затем выведет готовые SQL команды.

---

## Способ 2: Вручную

### Шаг 1 — Сгенерировать bcrypt hash пароля

**Python:**
```bash
python3 -c "
import bcrypt
password = input('Password: ').encode()
hashed = bcrypt.hashpw(password, bcrypt.gensalt(12))
print(hashed.decode())
"
```

Если `bcrypt` не установлен:
```bash
pip3 install bcrypt
```

**Альтернатива через htpasswd (Apache utils):**
```bash
htpasswd -bnBC 12 "" 'ВашПароль' | tr -d ':\n'
```

### Шаг 2 — Подключиться к БД

**Через docker exec:**
```bash
docker exec -it postgres psql -U wizard_user -d auth_service_db
```

**Или через docker compose (локально):**
```bash
docker compose -f docker/docker-compose.local.yml exec postgres \
  psql -U wizard_user -d auth_service_db
```

### Шаг 3 — Выполнить SQL

```sql
-- Заменить значения на свои
-- ВАЖНО: hash должен начинаться с $2a$ (bcrypt формат)
BEGIN;

INSERT INTO users (email, username, password_hash, enabled)
VALUES ('admin@example.com', 'admin', '$2a$12$REPLACE_WITH_HASH', true);

INSERT INTO user_roles (user_id, role)
SELECT id, 'ADMIN' FROM users WHERE email = 'admin@example.com';

COMMIT;
```

### Шаг 4 — Проверить

```sql
SELECT u.id, u.email, u.username, u.enabled, ur.role
FROM users u
JOIN user_roles ur ON ur.user_id = u.id
WHERE u.email = 'admin@example.com';
```

---

## После создания первого администратора

Войти через cookie-based login:

```bash
curl -X POST http://localhost:8083/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"ВашПароль"}' \
  -c cookies.txt
```

Ответ содержит `accessToken`. Передавать как `Authorization: Bearer <token>` в admin endpoints.

Следующих администраторов создавать через API:

```bash
curl -X POST http://localhost:8083/api/v1/admin/users \
  -H "Authorization: Bearer <accessToken>" \
  -H "Content-Type: application/json" \
  -d '{"email":"admin2@example.com","username":"admin2","password":"SecurePass123"}'
```

---

## Admin API Endpoints

| Method | Path | Описание |
|--------|------|---------|
| `GET` | `/api/v1/admin/users` | Список пользователей (фильтр, сортировка, пагинация) |
| `POST` | `/api/v1/admin/users` | Создать нового администратора |
| `POST` | `/api/v1/admin/users/{id}/block` | Заблокировать пользователя |
| `POST` | `/api/v1/admin/users/{id}/unblock` | Разблокировать пользователя |

Параметры `GET /api/v1/admin/users`:
- `email` — фильтр по email (необязательный, ILIKE поиск)
- `page` — номер страницы (default: 0)
- `size` — размер страницы (default: 20, max: 100)
- `sortBy` — поле сортировки: `createdAt` (default) или `email`
- `sortDir` — направление: `ASC` или `DESC` (default: `DESC`)
