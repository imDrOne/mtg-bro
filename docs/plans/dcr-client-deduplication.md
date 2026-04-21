# DCR Client Deduplication (Draft)

## Context

Dynamic Client Registration (RFC 7591) был выбран как единственный рабочий способ подключения Claude Web к MCP: Phase 1 (public client с пустым `client_secret`) не заработал — Claude Web отклонял подключение. DCR остался обязательным.

**Проблема:** каждое подключение Claude Web вызывает `POST /connect/register` и создаёт новую строку в `oauth2_registered_client`. За несколько месяцев таблица засорится сотнями клиентов от одного пользователя.

Цель: сохранить DCR (без него Claude не подключается), но переиспользовать уже зарегистрированных клиентов где возможно.

---

## Варианты

### 1. Дедупликация по `redirect_uris` + `client_name`

В `DynamicClientRegistrationService.register()` (`auth-service/src/main/kotlin/xyz/candycrawler/authservice/application/service/DynamicClientRegistrationService.kt`) перед созданием нового клиента искать существующего с теми же `redirect_uris` и `client_name`:
- **Найден** → вернуть старый `client_id` + свежесгенерированный `client_secret` (обновить запись в БД).
- **Не найден** → создавать как сейчас.

**Спорный момент:** RFC 7591 предполагает новый `client_id` при каждом register. Нужно экспериментально проверить, переживёт ли Claude Web возврат ранее выданного `client_id` вместо нового — поведение не задокументировано.

### 2. TTL + GC (сборка мусора)

Фоновая задача удаляет клиентов старше N дней, у которых нет активных токенов в `oauth2_authorization`:
```sql
DELETE FROM oauth2_registered_client
WHERE client_id NOT IN (
    SELECT registered_client_id FROM oauth2_authorization
    WHERE access_token_expires_at > now()
)
AND client_id_issued_at < now() - INTERVAL '30 days'
AND client_id != 'mcp-client-001';  -- не трогать pre-seeded клиента
```
Безопаснее с точки зрения RFC compliance, но не решает накопление в реальном времени.

### 3. Гибрид (рекомендуется)

Дедупликация для одного fingerprint (те же `redirect_uris` → очевидно один и тот же MCP-клиент) + периодический GC для старых неактивных клиентов.

---

## Риски

| Риск | Описание |
|------|----------|
| RFC 7591 compliance | Reuse `client_id` нарушает стандарт; Claude может ожидать уникальности |
| Клиент-изоляция | Два разных пользователя с одинаковым `redirect_uri` (теоретически возможно) не должны шарить `client_id` — нужна дополнительная изоляция |
| Сложность | Поиск по `redirect_uris` нетривиален: поле `varchar(1000)`, порядок URI может различаться — нужна нормализация |

---

## Ключевые файлы

- `auth-service/src/main/kotlin/xyz/candycrawler/authservice/application/service/DynamicClientRegistrationService.kt` — точка правки (логика lookup before insert).
- Новая миграция: индекс для GC-запроса по `client_id_issued_at`.

---

## Оценка

- 2-4 часа: локальный эксперимент с Claude Web (проверить, принимает ли Claude reused `client_id`).
- +1 день: если дедупликация работает — реализовать + тесты.
- GC-скрипт: ~2 часа отдельно.

## Статус

**Draft.** Запускать после завершения Phase 2 (RBAC).
