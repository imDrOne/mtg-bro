# mtg-bro — Инструкция по развёртыванию

## Архитектура CI/CD

Каждый модуль деплоится независимо по тегу `<module>/v<semver>`.

```
build-and-push ──────────────────────┐
                                      ├──► run-migrations ──► deploy
build-migrations (пропускается для   │
mcp-server)  ────────────────────────┘
```

1. **build-and-push** — JIB собирает multi-platform (amd64 + arm64) app-образ, пушит в `ghcr.io/<owner>/mtg-bro/<module>:latest`
2. **build-migrations** — Docker buildx собирает Liquibase-образ из `docker/migrations/Dockerfile`
3. **run-migrations** — пишет `.<module>.env` на сервер → запускает Liquibase через SSH; для `draftsim-parser` дополнительно копирует и запускает Qdrant schema migration
4. **deploy** — пишет `.<module>.env` на сервер → `docker compose pull && up -d --no-deps <module>`

Переменные окружения модуля хранятся в GitHub Environment secrets и автоматически попадают в `.<module>.env` на сервере при каждом деплое через `toJSON(secrets)`. Добавить новую переменную = добавить секрет в GitHub Environment, `_deploy-module.yml` не трогается.

---

## 1. Подготовка сервера (один раз)

Запусти скрипт из корня репозитория прямо на сервере:

```bash
bash scripts/setup-server.sh
```

Скрипт выполняет:
- создаёт `/opt/mtg-bro/docker/` и `/opt/mtg-bro/postgres/data/`
- выставляет права на директории
- создаёт Docker network `mtg-bro-net`
- копирует docker-файлы из репозитория в `/opt/mtg-bro/docker/`
- генерирует SSH deploy-ключ и печатает base64 для GitHub secret

После запуска скрипта выполни вручную:

### 1.1 Запустить PostgreSQL

```bash
cd /opt/mtg-bro/docker/postgres
DB_USERNAME=<user> DB_PASSWORD=<password> docker compose up -d
```

> При первом запуске с пустым volume автоматически создаются БД:
> `collection_manager_db`, `draftsim_parser_db`, `wizard_stat_db`, `auth_service_db`

Проверить:
```bash
docker exec postgres psql -U <user> -d postgres -c "\l"
```

### 1.2 Добавить GitHub Secrets

**Settings → Secrets and variables → Actions → New repository secret**

Только CI-механика — одинаковы для всех модулей:

| Секрет | Значение |
|--------|----------|
| `SSH_HOST` | IP адрес сервера |
| `SSH_USER` | Пользователь (например `drone`) |
| `SSH_PRIVATE_KEY` | base64-encoded приватный ключ (напечатан скриптом) |
| `GHCR_TOKEN` | GitHub PAT с правами `read:packages` |

### 1.3 Создать GitHub Environments

**Settings → Environments → New environment**

Создать четыре окружения и добавить secrets в каждое:

#### `production-collection-manager`

| Секрет | Значение |
|--------|----------|
| `DB_HOST` | `postgres` |
| `DB_PORT` | `5432` (опционально) |
| `DB_NAME` | `collection_manager_db` |
| `DB_USERNAME` | Пользователь PostgreSQL |
| `DB_PASSWORD` | Пароль PostgreSQL |
| `AUTH_ISSUER_URI` | Публичный URL auth-service, например `https://auth.duckdns.org` **(обязательно)** |

#### `production-draftsim-parser`

| Секрет | Значение |
|--------|----------|
| `DB_HOST` | `postgres` |
| `DB_PORT` | `5432` (опционально) |
| `DB_NAME` | `draftsim_parser_db` |
| `DB_USERNAME` | Пользователь PostgreSQL |
| `DB_PASSWORD` | Пароль PostgreSQL |
| `LLM_CLIENT` | `SPRING_AI` |
| `AI_CHAT_PROVIDER` | `anthropic` |
| `AI_CHAT_MODEL` | Например `claude-haiku-4-5-20251001` |
| `ANTHROPIC_API_KEY` | API ключ Anthropic для анализа статей |
| `AI_EMBEDDING_PROVIDER` | `openai` |
| `AI_EMBEDDING_MODEL` | Например `text-embedding-3-small` |
| `OPENAI_API_KEY` | API ключ OpenAI для embeddings |
| `QDRANT_HOST` | `qdrant` |
| `QDRANT_PORT` | `6334` |
| `QDRANT_COLLECTION` | `draftsim_article_insights_v1` |
| `QDRANT_VECTOR_SIZE` | `1536` для `text-embedding-3-small` |
| `QDRANT_DISTANCE` | `Cosine` |
| `ARTICLE_VECTOR_INDEX_ENABLED` | `true` |
| `AUTH_ISSUER_URI` | Публичный URL auth-service, например `https://auth.duckdns.org` **(обязательно)** |

GitHub pipeline выполняет явную миграцию Qdrant автоматически при деплое `draftsim-parser`. Для ручного запуска или восстановления:

```bash
cd /opt/mtg-bro
QDRANT_URL=http://127.0.0.1:6333 \
QDRANT_COLLECTION=draftsim_article_insights_v1 \
QDRANT_VECTOR_SIZE=1536 \
QDRANT_DISTANCE=Cosine \
AI_EMBEDDING_MODEL=text-embedding-3-small \
python3 scripts/qdrant-migrate.py
```

#### `production-wizard-stat-aggregator`

| Секрет | Значение |
|--------|----------|
| `DB_HOST` | `postgres` |
| `DB_PORT` | `5432` (опционально) |
| `DB_NAME` | `wizard_stat_db` |
| `DB_USERNAME` | Пользователь PostgreSQL |
| `DB_PASSWORD` | Пароль PostgreSQL |
| `AUTH_ISSUER_URI` | Публичный URL auth-service, например `https://auth.duckdns.org` **(обязательно)** |

#### `production-mcp-server`

| Секрет | Значение |
|--------|----------|
| `COLLECTION_MANAGER_BASE_URL` | `http://collection-manager:8080` |
| `DRAFTSIM_PARSER_BASE_URL` | `http://draftsim-parser:8081` |
| `AUTH_ISSUER_URI` | Публичный URL auth-service, например `https://auth.duckdns.org` |
| `MCP_BASE_URL` | Публичный URL MCP-сервера, например `https://mtg-bro.duckdns.org` |

#### `production-infra`

Секреты для reverse proxy (Caddy) и DuckDNS. Деплоится тегом `caddy/v*` независимо от приложений.

| Секрет | Значение |
|--------|----------|
| `MCP_DOMAIN` | Домен для MCP-сервера, например `mtg-bro.duckdns.org` |
| `AUTH_DOMAIN` | Домен для auth-service, например `auth.duckdns.org` |
| `DUCKDNS_TOKEN` | Токен с duckdns.org (используется Caddy для DNS-01 TLS challenge) |
| `CADDY_EMAIL` | Email для Let's Encrypt |

#### `production-auth-service`

| Секрет | Значение |
|--------|----------|
| `DB_HOST` | `postgres` |
| `DB_PORT` | `5432` (опционально) |
| `DB_NAME` | `auth_service_db` |
| `DB_USERNAME` | Пользователь PostgreSQL |
| `DB_PASSWORD` | Пароль PostgreSQL |
| `AUTH_ISSUER_URI` | Публичный URL auth-service, например `https://auth.duckdns.org` |
| `MCP_CLIENT_REDIRECT_URI` | Redirect URI MCP-клиента, например `https://claude.ai/api/mcp/auth_callback` |
| `AUTH_TRUSTED_PROXY_CIDR` | CIDR доверенного прокси (дефолт `172.16.0.0/12`) |
| `AUTH_REFRESH_COOKIE_SECURE` | Флаг `Secure` на refresh-cookie; дефолт `true` — **не менять в prod** |

---

## 2. Деплой модуля

```bash
git tag collection-manager/v1.0.0
git push origin collection-manager/v1.0.0

git tag draftsim-parser/v1.0.0
git push origin draftsim-parser/v1.0.0

git tag wizard-stat-aggregator/v1.0.0
git push origin wizard-stat-aggregator/v1.0.0

git tag mcp-server/v1.0.0
git push origin mcp-server/v1.0.0

git tag auth-service/v1.0.0
git push origin auth-service/v1.0.0

git tag caddy/v1.0.0
git push origin caddy/v1.0.0
```

После пуша тега GitHub Actions запускает соответствующий workflow автоматически.

---

## 3. Управление переменными окружения

**Добавить новую переменную**: Settings → Environments → `production-<module>` → Add secret.
При следующем деплое она автоматически появится в `.<module>.env` на сервере.

Файлы на сервере (создаются CI автоматически):
```
/opt/mtg-bro/docker/
  .collection-manager.env
  .draftsim-parser.env
  .wizard-stat-aggregator.env
  .mcp-server.env
  .auth-service.env
  .infra.env
```

---

## 4. Полезные команды

```bash
# Статус контейнеров
docker ps

# Логи модуля
docker compose -f /opt/mtg-bro/docker/docker-compose.prod.yml logs -f collection-manager

# Перезапустить модуль вручную
cd /opt/mtg-bro/docker
docker compose -f docker-compose.prod.yml restart collection-manager

# Проверить env-файл модуля
cat /opt/mtg-bro/docker/.collection-manager.env

# Проверить БД
docker exec postgres psql -U <DB_USERNAME> -d postgres -c "\l"

# Локальный доступ к БД через SSH-туннель
ssh -L 5433:127.0.0.1:5433 drone@<server-ip> -N
# затем подключаться к localhost:5433
```
