# Agent-driven onboarding for mtg-bro

Этот документ - сценарий интерактивного онбординга по проекту `mtg-bro`.
Он рассчитан на запуск внутри Claude Code, Codex или другого агента, который
может читать репозиторий и вести пошаговый чат с новым сотрудником.

## Как запустить

Открой агента в корне репозитория и напиши:

```text
Следуй docs/onboarding/agent-onboarding.md и проведи меня по онбордингу.
```

Агент должен показывать по одному шагу, ждать `Next`, `Yes`, `Done` или похожее
подтверждение, и только после этого переходить дальше.

## Инструкции для агента

Ты ведешь нового сотрудника по проекту `mtg-bro`.

Правила:

- Не показывай весь документ сразу.
- На каждом шаге выводи: номер шага, цель, что открыть или выполнить, что нужно
  понять перед переходом дальше.
- После каждого шага жди подтверждение пользователя: `Next`, `Yes`, `Done`,
  `Дальше`, `Готово` или аналогичную фразу.
- Если команда может занять время или требует Docker/сеть/секреты, сначала
  объясни это и предложи пропустить практическую часть.
- Не запускай деплой, `git push`, создание тегов, запись секретов или изменение
  миграций во время онбординга.
- Если пользователь просит углубиться в модуль, используй ближайший
  `CLAUDE.md`, README и код модуля как источник правды.
- Если какой-то путь или команда устарели, остановись, найди актуальный путь в
  репозитории и коротко объясни расхождение.

Формат каждого сообщения:

```text
Шаг N/M: <название>
Цель: <1-2 предложения>
Сделай:
- <конкретное действие>
- <конкретное действие>
Перед переходом убедись, что понятно: <ожидаемый результат>

Напиши "Готово", когда можно идти дальше.
```

## Шаги онбординга

### Шаг 1/10: Картина проекта

Цель: понять, что делает продукт и из каких сервисов он состоит.

Сделай:

- Открой `CLAUDE.md`.
- Найди раздел `Project Overview`.
- Прочитай краткое назначение модулей:
  `collection-manager`, `draftsim-parser`, `wizard-stat-aggregator`,
  `auth-service`, `mcp-server`, `common`.

Перед переходом убедись, что понятно:

- `mtg-bro` - это multi-module Kotlin/Gradle проект для MTG deck-building.
- Большинство сервисов - Spring Boot + PostgreSQL.
- `mcp-server` - отдельный Ktor/MCP сервер без собственной БД.

### Шаг 2/10: Карта документации

Цель: понять, где искать инструкции по проекту и модулям.

Сделай:

- Открой корневой `CLAUDE.md`.
- Открой модульные документы:
  `collection-manager/CLAUDE.md`,
  `auth-service/CLAUDE.md`,
  `draftsim-parser/CLAUDE.md`,
  `wizard-stat-aggregator/CLAUDE.md`,
  `mcp-server/CLAUDE.md`.
- Открой `mcp-server/README.md`, если работа будет касаться MCP или Claude
  connector.

Перед переходом убедись, что понятно:

- Корневой `CLAUDE.md` описывает общие правила, команды и архитектуру.
- Модульные `CLAUDE.md` уточняют назначение, env vars, auth и тестовые правила
  конкретного сервиса.
- `docs/deploy.md` нужен для деплоя, но деплой не выполняется во время
  онбординга.

### Шаг 3/10: Локальная среда и команды

Цель: увидеть минимальный набор команд для сборки, тестов и локального запуска.

Сделай:

- Открой `settings.gradle.kts` и проверь список Gradle-модулей.
- Открой корневой `build.gradle.kts` и найди задачи `jibDockerBuild` и
  `runLocal`.
- Посмотри основные команды:

```bash
./gradlew build
./gradlew test
./gradlew :mcp-server:installDist
./gradlew runLocal
```

Перед переходом убедись, что понятно:

- `./gradlew test` запускает тесты всех модулей.
- `./gradlew runLocal` строит Docker images и поднимает локальный стек через
  Docker Compose.
- `runLocal` требует Docker, а для публичного Claude web connector обычно нужен
  `NGROK_AUTHTOKEN`.

### Шаг 4/10: Порты и локальный стек

Цель: понять, какие сервисы доступны при локальном запуске.

Сделай:

- В `build.gradle.kts` найди вывод `runLocal`.
- Сопоставь сервисы и порты:
  `collection-manager` - `http://localhost:8080`,
  `draftsim-parser` - `http://localhost:8081`,
  `wizard-stat-aggregator` - `http://localhost:8082`,
  `auth-service` - `http://localhost:8083`,
  `mcp-server` - `http://localhost:3000/mcp`.
- Открой `docker/docker-compose.local.yml` для общего понимания состава
  локального окружения.

Перед переходом убедись, что понятно:

- Каждый Spring-сервис имеет свою PostgreSQL database.
- MCP server общается с backend-сервисами по HTTP.
- Swagger UI доступен у Spring-сервисов, но защищенные endpoints требуют JWT.

### Шаг 5/10: Auth и permissions

Цель: понять, как пользователь получает JWT и как сервисы проверяют доступ.

Сделай:

- Открой `auth-service/CLAUDE.md`.
- Прочитай разделы про регистрацию, cookie-based login flow и OAuth 2.0
  Authorization Code + PKCE.
- Открой `auth-service/src/test/LOCAL_LOGIN.md` как практический reference для
  локального логина.
- В корневом `CLAUDE.md` найди раздел `API Permissions`.

Перед переходом убедись, что понятно:

- `auth-service` выдает JWT с claims `roles` и `permissions`.
- Resource services мапят permissions в authorities вида `PERM_<permission>`.
- Для Swagger запросов нужно получить access token и вставить его через
  `Authorize`.

### Шаг 6/10: Collection manager flow

Цель: понять основной пользовательский поток коллекции карт.

Сделай:

- Открой `collection-manager/CLAUDE.md`.
- Прочитай разделы `Module Purpose`, `Import pipeline`, `Card search`,
  `Multi-tenancy`.
- Обрати внимание на Scryfall batch limit и user-scoped данные.

Перед переходом убедись, что понятно:

- Импорт читает TCGPlayer/Moxfield exports, обогащает карты через Scryfall и
  сохраняет коллекцию.
- `cards` - общий каталог, а `collection_entries`, `decks`, `deck_entries`
  scoped by `user_id`.
- Репозитории работают с Domain Entities, а SQL records не выходят из DB layer.

### Шаг 7/10: MCP server и агентские tools

Цель: понять, как Claude/Codex получает доступ к данным проекта через MCP.

Сделай:

- Открой `mcp-server/CLAUDE.md` и `mcp-server/README.md`.
- Найди список MCP tools:
  `search_my_cards`, `search_scryfall`, `get_card`,
  `list_scryfall_format_codes`, `get_deckbuilding_guide`,
  `list_draftsim_articles`, `search_draftsim_articles`,
  `get_draftsim_articles`.
- Открой `mcp-server/src/main/resources/guides/deckbuilding/SKILL.md`.

Перед переходом убедись, что понятно:

- `stdio` transport используется для локальных IDE-клиентов.
- `http` transport используется для Claude web connector.
- OAuth включается для HTTP transport только когда заданы `AUTH_ISSUER_URI` и
  `MCP_BASE_URL`.

### Шаг 8/10: Draftsim и wizard stats

Цель: понять вспомогательные сервисы, которые дают данные для deck-building.

Сделай:

- Открой `draftsim-parser/CLAUDE.md`.
- Открой `wizard-stat-aggregator/CLAUDE.md`.
- Сравни их роли:
  `draftsim-parser` анализирует статьи Draftsim через LLM/Spring AI и индексирует
  insights;
  `wizard-stat-aggregator` собирает limited statistics из 17lands.

Перед переходом убедись, что понятно:

- `draftsim-parser` может требовать LLM/embedding ключи и Qdrant для полного
  production-like flow.
- `wizard-stat-aggregator` может работать по scheduler и хранит tracked limited
  sets.
- Оба сервиса защищены JWT и используют тот же общий подход к permissions.

### Шаг 9/10: Тесты, миграции и проверка изменений

Цель: понять минимальную проверку перед изменениями и правила работы с БД.

Сделай:

- В корневом `CLAUDE.md` найди разделы `Testing` и `Database Migrations`.
- Посмотри примеры команд:

```bash
./gradlew test
./gradlew :collection-manager:test
./gradlew :wizard-stat-aggregator:test
./gradlew :draftsim-parser:test
./gradlew :collection-manager:createMigration -PsqlName=add_new_table
```

Перед переходом убедись, что понятно:

- Integration tests используют Testcontainers PostgreSQL.
- SQL migrations лежат в
  `<module>/src/main/resources/db/changelog/migrations/`.
- Миграции нельзя изменять без явной задачи; новые изменения БД оформляются
  новой migration.

### Шаг 10/10: Деплой и рабочие правила

Цель: понять, как проект доставляется и какие архитектурные правила нельзя
ломать в повседневной работе.

Сделай:

- Открой `docs/deploy.md`, но не запускай deploy-команды.
- В корневом `CLAUDE.md` перечитай раздел `Architecture Rules`.
- Зафиксируй основные правила:
  Domain Entity crosses layer boundaries;
  SQL Mapper returns DB records;
  Repository maps records to Domain Entities;
  command repositories mutate, query repositories are read-only;
  protected APIs use permission-based access control.

Перед завершением убедись, что понятно:

- Деплой tag-driven: tags вида `<module>/v<semver>` запускают GitHub Actions.
- `scripts/deploy.py` - интерактивный deploy helper, не onboarding command.
- Перед завершением рабочих задач нужно запускать минимальную полезную проверку:
  tests, syntax checks или ручную проверку затронутого flow.

## Финальное сообщение агента

Когда пользователь прошел все шаги, выведи короткое резюме:

```text
Онбординг завершен.

Теперь ты должен понимать:
- назначение всех модулей mtg-bro;
- как локально собрать, протестировать и запустить проект;
- как устроены auth, permissions и MCP connector;
- где лежат модульные инструкции;
- какие архитектурные правила соблюдать при изменениях.

Для следующего шага выбери конкретный модуль или задачу, и агент сможет провести
уже task-specific onboarding.
```
