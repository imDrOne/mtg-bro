# OAuth Public Client + RBAC Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Перевести `mcp-client` из confidential в public OAuth-клиент и добавить RBAC (роли FREE/PRO/ADMIN) для разграничения доступа к MCP-тулам.

**Architecture:** auth-service добавляет таблицу `user_roles`, включает роль `FREE` по умолчанию при регистрации, и инъектирует `roles` claim в JWT access token. mcp-server извлекает роли из JWT через coroutine context, проверяет их перед выполнением каждого PRO-тула, и фильтрует список тулов в `tools/list` — так что FREE-пользователь физически не видит PRO-инструменты.

**Tech Stack:** Spring Authorization Server (OAuth2TokenCustomizer), NamedParameterJdbcTemplate, Ktor ApplicationPlugin + coroutineContext, Auth0 JWT library.

---

## Затронутые файлы

### auth-service
| Статус | Файл | Ответственность |
|--------|------|-----------------|
| Создать | `src/main/resources/db/changelog/migrations/20260417000004_make_mcp_client_public.sql` | Убрать secret у mcp-client |
| Создать | `src/main/resources/db/changelog/migrations/20260417000005_create_user_roles.sql` | Таблица user_roles |
| Создать | `src/main/kotlin/.../domain/user/model/UserRole.kt` | Enum FREE/PRO/ADMIN |
| Создать | `src/main/kotlin/.../domain/user/repository/UserRoleRepository.kt` | Интерфейс репозитория ролей |
| Создать | `src/main/kotlin/.../infrastructure/db/entity/UserRoleRecord.kt` | DB entity для user_roles |
| Создать | `src/main/kotlin/.../infrastructure/db/mapper/sql/UserRoleSqlMapper.kt` | SQL mapper для user_roles |
| Создать | `src/main/kotlin/.../infrastructure/db/repository/JdbcUserRoleRepository.kt` | Реализация репозитория |
| Изменить | `src/main/kotlin/.../security/AuthorizationServerConfig.kt` | Добавить JWT customizer bean |
| Изменить | `src/main/kotlin/.../application/service/UserRegistrationService.kt` | Назначать FREE при регистрации |
| Создать | `src/test/kotlin/.../infrastructure/db/mapper/UserRoleSqlMapperTest.kt` | Интеграционный тест маппера |
| Создать | `src/test/kotlin/.../infrastructure/db/repository/JdbcUserRoleRepositoryTest.kt` | Юнит-тест репозитория |
| Создать | `src/test/kotlin/.../application/service/UserRegistrationServiceRoleTest.kt` | Юнит-тест присвоения роли |

### mcp-server
| Статус | Файл | Ответственность |
|--------|------|-----------------|
| Создать | `src/main/kotlin/.../auth/UserRolesContext.kt` | CoroutineContext element + хелперы |
| Создать | `src/main/kotlin/.../auth/ToolAccessConfig.kt` | Матрица доступа роль → тулы |
| Изменить | `src/main/kotlin/.../auth/McpAuthPlugin.kt` | Извлекать roles из JWT в call.attributes |
| Изменить | `src/main/kotlin/.../Main.kt` | Инъектировать roles в coroutineContext |
| Изменить | `src/main/kotlin/.../McpServer.kt` | Проверять роли перед каждым PRO/ADMIN тулом |
| Создать | `src/main/kotlin/.../FilteredMcpServer.kt` | Подкласс Server с фильтрацией tools/list |
| Создать | `src/test/kotlin/.../auth/ToolAccessConfigTest.kt` | Юнит-тест матрицы доступа |
| Создать | `src/test/kotlin/.../FilteredMcpServerTest.kt` | Юнит-тест фильтрации tools/list |

---

## Фаза 1: Public Client

### Task 1: Перевести mcp-client в public

**Контекст:** `mcp-client` зарегистрирован в таблице `oauth2_registered_client` через Liquibase-миграцию `20260412000002_create_oauth2_tables.sql`. Текущий `client_authentication_methods = 'client_secret_basic'`. Нужно поставить `'none'` и обнулить секрет.

**Files:**
- Create: `auth-service/src/main/resources/db/changelog/migrations/20260417000004_make_mcp_client_public.sql`

- [ ] **Step 1: Создать миграцию**

```sql
-- liquibase formatted sql
-- changeset a.tikholoz:20260417000004

UPDATE oauth2_registered_client
SET client_secret                 = NULL,
    client_authentication_methods = 'none'
WHERE client_id = 'mcp-client';
```

- [ ] **Step 2: Проверить, что миграция включена в master**

Открыть `auth-service/src/main/resources/db/changelog/db.changelog-master.yaml`.
Убедиться, что там есть `include: { file: migrations/20260417000004_make_mcp_client_public.sql, ... }`.
Если нет — добавить по аналогии с предыдущими записями.

- [ ] **Step 3: Запустить миграцию локально**

```bash
./gradlew :auth-service:update
```

Ожидаемый вывод: `Liquibase: Update has been successful.`

- [ ] **Step 4: Commit**

```bash
git add auth-service/src/main/resources/db/changelog/
git commit -m "feat(auth): convert mcp-client to public OAuth client (no secret)"
```

---

## Фаза 2: RBAC — auth-service

### Task 2: Таблица user_roles + UserRole enum

**Files:**
- Create: `auth-service/src/main/resources/db/changelog/migrations/20260417000005_create_user_roles.sql`
- Create: `auth-service/src/main/kotlin/xyz/candycrawler/authservice/domain/user/model/UserRole.kt`

- [ ] **Step 1: Написать failing-тест для UserRole**

Создать `auth-service/src/test/kotlin/xyz/candycrawler/authservice/domain/user/model/UserRoleTest.kt`:

```kotlin
package xyz.candycrawler.authservice.domain.user.model

import kotlin.test.Test
import kotlin.test.assertEquals

class UserRoleTest {

    @Test
    fun `FREE is the default role`() {
        assertEquals("FREE", UserRole.FREE.name)
    }

    @Test
    fun `all roles are defined`() {
        val roles = UserRole.entries.map { it.name }
        assertEquals(listOf("FREE", "PRO", "ADMIN"), roles)
    }
}
```

- [ ] **Step 2: Запустить тест — убедиться, что не компилируется**

```bash
./gradlew :auth-service:test --tests "*.UserRoleTest" 2>&1 | head -20
```

Ожидаемый результат: ошибка компиляции `Unresolved reference: UserRole`.

- [ ] **Step 3: Создать UserRole enum**

Создать `auth-service/src/main/kotlin/xyz/candycrawler/authservice/domain/user/model/UserRole.kt`:

```kotlin
package xyz.candycrawler.authservice.domain.user.model

enum class UserRole {
    FREE,
    PRO,
    ADMIN,
}
```

- [ ] **Step 4: Создать миграцию для user_roles**

Создать `auth-service/src/main/resources/db/changelog/migrations/20260417000005_create_user_roles.sql`:

```sql
-- liquibase formatted sql
-- changeset a.tikholoz:20260417000005

CREATE TABLE user_roles (
    user_id    BIGINT      NOT NULL REFERENCES users(id),
    role       VARCHAR(50) NOT NULL,
    granted_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, role)
);

CREATE INDEX idx_user_roles_user_id ON user_roles (user_id);

-- Assign FREE to all existing users (idempotent)
INSERT INTO user_roles (user_id, role)
SELECT id, 'FREE'
FROM users
ON CONFLICT DO NOTHING;
```

- [ ] **Step 5: Запустить тест — убедиться, что проходит**

```bash
./gradlew :auth-service:test --tests "*.UserRoleTest"
```

Ожидаемый результат: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit**

```bash
git add auth-service/src/main/kotlin/xyz/candycrawler/authservice/domain/user/model/UserRole.kt
git add auth-service/src/main/resources/db/changelog/migrations/20260417000005_create_user_roles.sql
git add auth-service/src/test/kotlin/xyz/candycrawler/authservice/domain/user/model/UserRoleTest.kt
git commit -m "feat(auth): add UserRole enum and user_roles migration"
```

---

### Task 3: UserRoleSqlMapper (интеграционный тест)

**Files:**
- Create: `auth-service/src/main/kotlin/xyz/candycrawler/authservice/infrastructure/db/entity/UserRoleRecord.kt`
- Create: `auth-service/src/main/kotlin/xyz/candycrawler/authservice/infrastructure/db/mapper/sql/UserRoleSqlMapper.kt`
- Create: `auth-service/src/test/kotlin/xyz/candycrawler/authservice/infrastructure/db/mapper/UserRoleSqlMapperTest.kt`

- [ ] **Step 1: Написать failing integration-тест**

Создать `auth-service/src/test/kotlin/xyz/candycrawler/authservice/infrastructure/db/mapper/UserRoleSqlMapperTest.kt`:

```kotlin
package xyz.candycrawler.authservice.infrastructure.db.mapper

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import xyz.candycrawler.authservice.infrastructure.db.entity.UserRecord
import xyz.candycrawler.authservice.infrastructure.db.mapper.sql.UserRoleSqlMapper
import xyz.candycrawler.authservice.infrastructure.db.mapper.sql.UserSqlMapper
import xyz.candycrawler.authservice.lib.AbstractIntegrationTest
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UserRoleSqlMapperTest : AbstractIntegrationTest() {

    @Autowired
    lateinit var userRoleSqlMapper: UserRoleSqlMapper

    @Autowired
    lateinit var userSqlMapper: UserSqlMapper

    private fun insertUser(email: String, username: String): Long {
        val record = UserRecord(null, email, username, "\$2a\$10\$hash", true, Instant.now())
        return userSqlMapper.insert(record).id!!
    }

    @Test
    fun `insert assigns a role to a user`() {
        val userId = insertUser("role1@example.com", "roleuser1")
        userRoleSqlMapper.insert(userId, "FREE")
        val roles = userRoleSqlMapper.selectByUserId(userId)
        assertEquals(listOf("FREE"), roles)
    }

    @Test
    fun `insert multiple roles to same user`() {
        val userId = insertUser("role2@example.com", "roleuser2")
        userRoleSqlMapper.insert(userId, "FREE")
        userRoleSqlMapper.insert(userId, "PRO")
        val roles = userRoleSqlMapper.selectByUserId(userId)
        assertEquals(2, roles.size)
        assertTrue(roles.containsAll(listOf("FREE", "PRO")))
    }

    @Test
    fun `insert is idempotent - duplicate role is ignored`() {
        val userId = insertUser("role3@example.com", "roleuser3")
        userRoleSqlMapper.insert(userId, "FREE")
        userRoleSqlMapper.insert(userId, "FREE") // should not throw
        val roles = userRoleSqlMapper.selectByUserId(userId)
        assertEquals(listOf("FREE"), roles)
    }

    @Test
    fun `selectByUserId returns empty list for user with no roles`() {
        val userId = insertUser("role4@example.com", "roleuser4")
        val roles = userRoleSqlMapper.selectByUserId(userId)
        assertTrue(roles.isEmpty())
    }
}
```

- [ ] **Step 2: Запустить тест — убедиться, что не компилируется**

```bash
./gradlew :auth-service:test --tests "*.UserRoleSqlMapperTest" 2>&1 | head -20
```

Ожидаемый результат: ошибка компиляции `Unresolved reference: UserRoleSqlMapper`.

- [ ] **Step 3: Создать UserRoleRecord**

Создать `auth-service/src/main/kotlin/xyz/candycrawler/authservice/infrastructure/db/entity/UserRoleRecord.kt`:

```kotlin
package xyz.candycrawler.authservice.infrastructure.db.entity

import java.time.Instant

data class UserRoleRecord(
    val userId: Long,
    val role: String,
    val grantedAt: Instant,
)
```

- [ ] **Step 4: Создать UserRoleSqlMapper**

Создать `auth-service/src/main/kotlin/xyz/candycrawler/authservice/infrastructure/db/mapper/sql/UserRoleSqlMapper.kt`:

```kotlin
package xyz.candycrawler.authservice.infrastructure.db.mapper.sql

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component

@Component
class UserRoleSqlMapper(
    private val jdbc: NamedParameterJdbcTemplate,
) {

    internal fun insert(userId: Long, role: String) {
        val sql = """
            INSERT INTO user_roles (user_id, role)
            VALUES (:userId, :role)
            ON CONFLICT DO NOTHING
        """.trimIndent()

        jdbc.update(
            sql,
            MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("role", role),
        )
    }

    internal fun selectByUserId(userId: Long): List<String> {
        val sql = "SELECT role FROM user_roles WHERE user_id = :userId ORDER BY granted_at"
        return jdbc.queryForList(sql, MapSqlParameterSource("userId", userId), String::class.java)
    }
}
```

- [ ] **Step 5: Запустить интеграционный тест**

```bash
./gradlew :auth-service:test --tests "*.UserRoleSqlMapperTest"
```

Ожидаемый результат: `BUILD SUCCESSFUL`, 4 теста прошли.

- [ ] **Step 6: Commit**

```bash
git add auth-service/src/main/kotlin/xyz/candycrawler/authservice/infrastructure/db/entity/UserRoleRecord.kt
git add auth-service/src/main/kotlin/xyz/candycrawler/authservice/infrastructure/db/mapper/sql/UserRoleSqlMapper.kt
git add auth-service/src/test/kotlin/xyz/candycrawler/authservice/infrastructure/db/mapper/UserRoleSqlMapperTest.kt
git commit -m "feat(auth): add UserRoleSqlMapper with integration tests"
```

---

### Task 4: UserRoleRepository + JdbcUserRoleRepository

**Files:**
- Create: `auth-service/src/main/kotlin/xyz/candycrawler/authservice/domain/user/repository/UserRoleRepository.kt`
- Create: `auth-service/src/main/kotlin/xyz/candycrawler/authservice/infrastructure/db/repository/JdbcUserRoleRepository.kt`
- Create: `auth-service/src/test/kotlin/xyz/candycrawler/authservice/infrastructure/db/repository/JdbcUserRoleRepositoryTest.kt`

- [ ] **Step 1: Написать failing unit-тест**

Создать `auth-service/src/test/kotlin/xyz/candycrawler/authservice/infrastructure/db/repository/JdbcUserRoleRepositoryTest.kt`:

```kotlin
package xyz.candycrawler.authservice.infrastructure.db.repository

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import xyz.candycrawler.authservice.domain.user.model.UserRole
import xyz.candycrawler.authservice.infrastructure.db.mapper.sql.UserRoleSqlMapper
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JdbcUserRoleRepositoryTest {

    private val mapper = mockk<UserRoleSqlMapper>()
    private val repository = JdbcUserRoleRepository(mapper)

    @Test
    fun `assignRole calls mapper with correct userId and role name`() {
        every { mapper.insert(42L, "FREE") } returns Unit

        repository.assignRole(42L, UserRole.FREE)

        verify { mapper.insert(42L, "FREE") }
    }

    @Test
    fun `findByUserId maps string roles to UserRole enum`() {
        every { mapper.selectByUserId(1L) } returns listOf("FREE", "PRO")

        val roles = repository.findByUserId(1L)

        assertEquals(listOf(UserRole.FREE, UserRole.PRO), roles)
    }

    @Test
    fun `findByUserId returns empty list when no roles`() {
        every { mapper.selectByUserId(99L) } returns emptyList()

        val roles = repository.findByUserId(99L)

        assertTrue(roles.isEmpty())
    }
}
```

- [ ] **Step 2: Запустить тест — убедиться, что не компилируется**

```bash
./gradlew :auth-service:test --tests "*.JdbcUserRoleRepositoryTest" 2>&1 | head -20
```

Ожидаемый результат: ошибка компиляции `Unresolved reference: JdbcUserRoleRepository`.

- [ ] **Step 3: Создать интерфейс UserRoleRepository**

Создать `auth-service/src/main/kotlin/xyz/candycrawler/authservice/domain/user/repository/UserRoleRepository.kt`:

```kotlin
package xyz.candycrawler.authservice.domain.user.repository

import xyz.candycrawler.authservice.domain.user.model.UserRole

interface UserRoleRepository {
    fun assignRole(userId: Long, role: UserRole)
    fun findByUserId(userId: Long): List<UserRole>
}
```

- [ ] **Step 4: Создать JdbcUserRoleRepository**

Создать `auth-service/src/main/kotlin/xyz/candycrawler/authservice/infrastructure/db/repository/JdbcUserRoleRepository.kt`:

```kotlin
package xyz.candycrawler.authservice.infrastructure.db.repository

import org.springframework.stereotype.Service
import xyz.candycrawler.authservice.domain.user.model.UserRole
import xyz.candycrawler.authservice.domain.user.repository.UserRoleRepository
import xyz.candycrawler.authservice.infrastructure.db.mapper.sql.UserRoleSqlMapper

@Service
class JdbcUserRoleRepository(
    private val userRoleSqlMapper: UserRoleSqlMapper,
) : UserRoleRepository {

    override fun assignRole(userId: Long, role: UserRole) {
        userRoleSqlMapper.insert(userId, role.name)
    }

    override fun findByUserId(userId: Long): List<UserRole> =
        userRoleSqlMapper.selectByUserId(userId).map { UserRole.valueOf(it) }
}
```

- [ ] **Step 5: Запустить тест**

```bash
./gradlew :auth-service:test --tests "*.JdbcUserRoleRepositoryTest"
```

Ожидаемый результат: `BUILD SUCCESSFUL`, 3 теста прошли.

- [ ] **Step 6: Commit**

```bash
git add auth-service/src/main/kotlin/xyz/candycrawler/authservice/domain/user/repository/UserRoleRepository.kt
git add auth-service/src/main/kotlin/xyz/candycrawler/authservice/infrastructure/db/repository/JdbcUserRoleRepository.kt
git add auth-service/src/test/kotlin/xyz/candycrawler/authservice/infrastructure/db/repository/JdbcUserRoleRepositoryTest.kt
git commit -m "feat(auth): add UserRoleRepository and JdbcUserRoleRepository"
```

---

### Task 5: JWT Customizer — добавить roles claim в access token

**Контекст:** Spring Authorization Server вызывает `OAuth2TokenCustomizer<JwtEncodingContext>` bean при создании JWT. `context.getPrincipal<Authentication>().name` возвращает email пользователя (UserDetailsServiceAdapter использует email как username). Нам нужно добавить `roles` claim в access token.

**Files:**
- Modify: `auth-service/src/main/kotlin/xyz/candycrawler/authservice/security/AuthorizationServerConfig.kt`

- [ ] **Step 1: Добавить JWT customizer bean в AuthorizationServerConfig**

Открыть `auth-service/src/main/kotlin/xyz/candycrawler/authservice/security/AuthorizationServerConfig.kt`.

Добавить импорты в начало файла:

```kotlin
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer
import xyz.candycrawler.authservice.domain.user.repository.UserRepository
import xyz.candycrawler.authservice.domain.user.repository.UserRoleRepository
```

Добавить зависимости в конструктор класса `AuthorizationServerConfig`:

```kotlin
@Configuration
class AuthorizationServerConfig(
    private val rsaKeySqlMapper: RsaKeySqlMapper,
    private val userRepository: UserRepository,
    private val userRoleRepository: UserRoleRepository,
) {
```

Добавить bean в конец класса (перед закрывающей `}`):

```kotlin
    @Bean
    fun jwtCustomizer(): OAuth2TokenCustomizer<JwtEncodingContext> =
        OAuth2TokenCustomizer { context ->
            if (context.tokenType == OAuth2TokenType.ACCESS_TOKEN) {
                val email = context.getPrincipal<Authentication>().name
                val user = userRepository.findByEmail(email) ?: return@OAuth2TokenCustomizer
                val roles = userRoleRepository.findByUserId(user.id!!)
                context.claims.claim("roles", roles.map { it.name })
            }
        }
```

- [ ] **Step 2: Запустить все тесты auth-service**

```bash
./gradlew :auth-service:test
```

Ожидаемый результат: `BUILD SUCCESSFUL`, все тесты прошли.

- [ ] **Step 3: Commit**

```bash
git add auth-service/src/main/kotlin/xyz/candycrawler/authservice/security/AuthorizationServerConfig.kt
git commit -m "feat(auth): add JWT customizer to include roles claim in access token"
```

---

### Task 6: Назначать роль FREE при регистрации

**Files:**
- Modify: `auth-service/src/main/kotlin/xyz/candycrawler/authservice/application/service/UserRegistrationService.kt`
- Create: `auth-service/src/test/kotlin/xyz/candycrawler/authservice/application/service/UserRegistrationServiceRoleTest.kt`

- [ ] **Step 1: Написать failing unit-тест**

Создать `auth-service/src/test/kotlin/xyz/candycrawler/authservice/application/service/UserRegistrationServiceRoleTest.kt`:

```kotlin
package xyz.candycrawler.authservice.application.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.password.PasswordEncoder
import xyz.candycrawler.authservice.domain.user.model.User
import xyz.candycrawler.authservice.domain.user.model.UserRole
import xyz.candycrawler.authservice.domain.user.repository.UserRepository
import xyz.candycrawler.authservice.domain.user.repository.UserRoleRepository
import java.time.Instant

class UserRegistrationServiceRoleTest {

    private val userRepository = mockk<UserRepository>()
    private val userRoleRepository = mockk<UserRoleRepository>()
    private val passwordEncoder = mockk<PasswordEncoder>()
    private val service = UserRegistrationService(userRepository, userRoleRepository, passwordEncoder)

    @Test
    fun `register assigns FREE role to new user`() {
        every { passwordEncoder.encode(any()) } returns "\$2a\$10\$hash"
        every { userRepository.existsByEmail(any()) } returns false
        every { userRepository.existsByUsername(any()) } returns false
        every { userRepository.save(any()) } returns User(
            id = 1L,
            email = "new@example.com",
            username = "newuser",
            passwordHash = "\$2a\$10\$hash",
            enabled = true,
            createdAt = Instant.now(),
        )
        every { userRoleRepository.assignRole(1L, UserRole.FREE) } returns Unit

        service.register("new@example.com", "newuser", "password123")

        verify { userRoleRepository.assignRole(1L, UserRole.FREE) }
    }
}
```

- [ ] **Step 2: Запустить тест — убедиться, что не компилируется**

```bash
./gradlew :auth-service:test --tests "*.UserRegistrationServiceRoleTest" 2>&1 | head -20
```

Ожидаемый результат: ошибка компиляции (UserRegistrationService не принимает UserRoleRepository).

- [ ] **Step 3: Обновить UserRegistrationService**

Открыть `auth-service/src/main/kotlin/xyz/candycrawler/authservice/application/service/UserRegistrationService.kt`.

Заменить содержимое файла:

```kotlin
package xyz.candycrawler.authservice.application.service

import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import xyz.candycrawler.authservice.domain.user.exception.UserInvalidException
import xyz.candycrawler.authservice.domain.user.model.User
import xyz.candycrawler.authservice.domain.user.model.UserRole
import xyz.candycrawler.authservice.domain.user.repository.UserRepository
import xyz.candycrawler.authservice.domain.user.repository.UserRoleRepository
import java.time.Instant

@Service
class UserRegistrationService(
    private val userRepository: UserRepository,
    private val userRoleRepository: UserRoleRepository,
    private val passwordEncoder: PasswordEncoder,
) {

    @Transactional
    fun register(email: String, username: String, rawPassword: String): User {
        if (rawPassword.length < 8) throw UserInvalidException("password must be at least 8 characters")

        if (userRepository.existsByEmail(email.lowercase().trim())) {
            throw UserInvalidException("email is already taken")
        }
        if (userRepository.existsByUsername(username.trim())) {
            throw UserInvalidException("username is already taken")
        }

        val user = User(
            id = null,
            email = email.lowercase().trim(),
            username = username.trim(),
            passwordHash = passwordEncoder.encode(rawPassword)!!,
            enabled = true,
            createdAt = Instant.now(),
        )

        val saved = userRepository.save(user)
        userRoleRepository.assignRole(saved.id!!, UserRole.FREE)
        return saved
    }
}
```

- [ ] **Step 4: Обновить существующий UserRegistrationServiceTest**

Открыть `auth-service/src/test/kotlin/xyz/candycrawler/authservice/application/service/UserRegistrationServiceTest.kt`.

В начало каждого теста в этом файле добавить мок для `userRoleRepository` и создать сервис с тремя параметрами. Сначала прочитать файл, затем добавить `userRoleRepository = mockk<UserRoleRepository>()` к приватным полям класса и обновить создание `UserRegistrationService` в тестах.

Конкретно: найти строку `val service = UserRegistrationService(userRepository, passwordEncoder)` или аналогичную инициализацию в тест-классе и заменить на:

```kotlin
private val userRoleRepository = mockk<UserRoleRepository>(relaxed = true)
// В каждом тесте, где вызывается register() и пользователь успешно сохраняется:
every { userRoleRepository.assignRole(any(), UserRole.FREE) } returns Unit
```

Также добавить импорт в файл: `import xyz.candycrawler.authservice.domain.user.repository.UserRoleRepository`

- [ ] **Step 5: Запустить все тесты**

```bash
./gradlew :auth-service:test
```

Ожидаемый результат: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit**

```bash
git add auth-service/src/main/kotlin/xyz/candycrawler/authservice/application/service/UserRegistrationService.kt
git add auth-service/src/test/kotlin/xyz/candycrawler/authservice/application/service/
git commit -m "feat(auth): assign FREE role to new users on registration"
```

---

## Фаза 2: RBAC — mcp-server

### Task 7: UserRolesContext + ToolAccessConfig

**Контекст:** Для передачи ролей из Ktor-плагина (McpAuthPlugin) в MCP-тул-хендлеры используется Kotlin coroutine context. McpAuthPlugin хранит роли в `call.attributes`. Main.kt обернёт pipeline в `withContext(UserRolesElement(roles))`, чтобы тул-хендлеры могли читать роли через `coroutineContext`.

**Files:**
- Create: `mcp-server/src/main/kotlin/xyz/candycrawler/mcpserver/auth/UserRolesContext.kt`
- Create: `mcp-server/src/main/kotlin/xyz/candycrawler/mcpserver/auth/ToolAccessConfig.kt`
- Create: `mcp-server/src/test/kotlin/xyz/candycrawler/mcpserver/auth/ToolAccessConfigTest.kt`

- [ ] **Step 1: Создать тест директорию**

```bash
mkdir -p mcp-server/src/test/kotlin/xyz/candycrawler/mcpserver/auth
```

- [ ] **Step 2: Написать failing unit-тест для ToolAccessConfig**

Создать `mcp-server/src/test/kotlin/xyz/candycrawler/mcpserver/auth/ToolAccessConfigTest.kt`:

```kotlin
package xyz.candycrawler.mcpserver.auth

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ToolAccessConfigTest {

    @Test
    fun `FREE user can access free tools`() {
        val roles = listOf("FREE")
        assertTrue(ToolAccessConfig.hasAccess("search_my_cards", roles))
        assertTrue(ToolAccessConfig.hasAccess("search_scryfall", roles))
        assertTrue(ToolAccessConfig.hasAccess("get_card", roles))
        assertTrue(ToolAccessConfig.hasAccess("list_scryfall_format_codes", roles))
    }

    @Test
    fun `FREE user cannot access pro tools`() {
        val roles = listOf("FREE")
        assertFalse(ToolAccessConfig.hasAccess("analyze_tribal_depth", roles))
        assertFalse(ToolAccessConfig.hasAccess("get_collection_overview", roles))
        assertFalse(ToolAccessConfig.hasAccess("search_draftsim_articles", roles))
        assertFalse(ToolAccessConfig.hasAccess("get_draftsim_articles", roles))
        assertFalse(ToolAccessConfig.hasAccess("save_deck", roles))
    }

    @Test
    fun `PRO user can access all tools`() {
        val roles = listOf("PRO")
        assertTrue(ToolAccessConfig.hasAccess("search_my_cards", roles))
        assertTrue(ToolAccessConfig.hasAccess("analyze_tribal_depth", roles))
        assertTrue(ToolAccessConfig.hasAccess("save_deck", roles))
    }

    @Test
    fun `ADMIN user can access all tools`() {
        val roles = listOf("ADMIN")
        assertTrue(ToolAccessConfig.hasAccess("search_my_cards", roles))
        assertTrue(ToolAccessConfig.hasAccess("analyze_tribal_depth", roles))
        assertTrue(ToolAccessConfig.hasAccess("save_deck", roles))
    }

    @Test
    fun `user with no roles cannot access any tool`() {
        assertFalse(ToolAccessConfig.hasAccess("search_my_cards", emptyList()))
        assertFalse(ToolAccessConfig.hasAccess("analyze_tribal_depth", emptyList()))
    }

    @Test
    fun `user with both FREE and PRO roles can access all tools`() {
        val roles = listOf("FREE", "PRO")
        assertTrue(ToolAccessConfig.hasAccess("search_my_cards", roles))
        assertTrue(ToolAccessConfig.hasAccess("analyze_tribal_depth", roles))
    }
}
```

- [ ] **Step 3: Запустить тест — убедиться, что не компилируется**

```bash
./gradlew :mcp-server:test --tests "*.ToolAccessConfigTest" 2>&1 | head -20
```

Ожидаемый результат: ошибка компиляции `Unresolved reference: ToolAccessConfig`.

- [ ] **Step 4: Создать UserRolesContext**

Создать `mcp-server/src/main/kotlin/xyz/candycrawler/mcpserver/auth/UserRolesContext.kt`:

```kotlin
package xyz.candycrawler.mcpserver.auth

import io.ktor.util.AttributeKey
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

val UserRolesKey = AttributeKey<List<String>>("UserRoles")

class UserRolesElement(val roles: List<String>) : AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<UserRolesElement>
}

suspend fun currentUserRoles(): List<String> =
    coroutineContext[UserRolesElement]?.roles ?: emptyList()
```

- [ ] **Step 5: Создать ToolAccessConfig**

Создать `mcp-server/src/main/kotlin/xyz/candycrawler/mcpserver/auth/ToolAccessConfig.kt`:

```kotlin
package xyz.candycrawler.mcpserver.auth

object ToolAccessConfig {

    private val FREE_TOOLS = setOf(
        "search_my_cards",
        "search_scryfall",
        "get_card",
        "list_scryfall_format_codes",
    )

    private val PRO_TOOLS = FREE_TOOLS + setOf(
        "analyze_tribal_depth",
        "get_collection_overview",
        "search_draftsim_articles",
        "get_draftsim_articles",
        "save_deck",
    )

    private val ACCESS: Map<String, Set<String>> = mapOf(
        "FREE" to FREE_TOOLS,
        "PRO" to PRO_TOOLS,
        "ADMIN" to PRO_TOOLS,
    )

    fun hasAccess(toolName: String, roles: List<String>): Boolean =
        roles.any { role -> ACCESS[role]?.contains(toolName) == true }
}
```

- [ ] **Step 6: Запустить тест**

```bash
./gradlew :mcp-server:test --tests "*.ToolAccessConfigTest"
```

Ожидаемый результат: `BUILD SUCCESSFUL`, 6 тестов прошли.

- [ ] **Step 7: Commit**

```bash
git add mcp-server/src/main/kotlin/xyz/candycrawler/mcpserver/auth/UserRolesContext.kt
git add mcp-server/src/main/kotlin/xyz/candycrawler/mcpserver/auth/ToolAccessConfig.kt
git add mcp-server/src/test/kotlin/xyz/candycrawler/mcpserver/auth/ToolAccessConfigTest.kt
git commit -m "feat(mcp): add UserRolesContext and ToolAccessConfig for RBAC"
```

---

### Task 8: Извлекать roles из JWT в McpAuthPlugin

**Контекст:** `McpAuthPlugin.kt` уже валидирует JWT и извлекает `decodedJwt`. Нужно добавить извлечение `roles` claim (который auth-service добавляет как `List<String>`) и сохранить в `call.attributes[UserRolesKey]`. Auth0 JWT `Claim.asList(String::class.java)` может вернуть `null` — нужно обработать.

**Files:**
- Modify: `mcp-server/src/main/kotlin/xyz/candycrawler/mcpserver/auth/McpAuthPlugin.kt`

- [ ] **Step 1: Обновить McpAuthPlugin для извлечения ролей**

Открыть `mcp-server/src/main/kotlin/xyz/candycrawler/mcpserver/auth/McpAuthPlugin.kt`.

Добавить импорт в начало файла (после существующих импортов):

```kotlin
import xyz.candycrawler.mcpserver.auth.UserRolesKey
```

Заменить блок `try { ... }` внутри `onCall` (строки 47–61):

```kotlin
        val token = authHeader.removePrefix("Bearer ").trim()
        try {
            val decodedJwt = JWT.decode(token)
            val jwk = jwkProvider.get(decodedJwt.keyId)
            val algorithm = Algorithm.RSA256(jwk.publicKey as RSAPublicKey, null)
            JWT.require(algorithm)
                .withIssuer(issuerUri)
                .build()
                .verify(decodedJwt)

            val roles = decodedJwt.getClaim("roles").asList(String::class.java) ?: emptyList()
            call.attributes.put(UserRolesKey, roles)
        } catch (e: Exception) {
            call.response.header(
                "WWW-Authenticate",
                """Bearer error="invalid_token", resource_metadata="$resourceMetadataUrl""""
            )
            call.respond(HttpStatusCode.Unauthorized)
        }
```

- [ ] **Step 2: Собрать проект**

```bash
./gradlew :mcp-server:build
```

Ожидаемый результат: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add mcp-server/src/main/kotlin/xyz/candycrawler/mcpserver/auth/McpAuthPlugin.kt
git commit -m "feat(mcp): extract roles claim from JWT into call attributes"
```

---

### Task 9: Инъектировать roles в coroutineContext в Main.kt

**Контекст:** Ktor `intercept(ApplicationCallPipeline.Plugins)` добавленный ПОСЛЕ `install(McpAuthPlugin)` выполняется после McpAuthPlugin и может обернуть оставшийся pipeline в `withContext(UserRolesElement(roles))`. Это гарантирует, что тул-хендлеры видят роли через `coroutineContext[UserRolesElement]`.

**Files:**
- Modify: `mcp-server/src/main/kotlin/xyz/candycrawler/mcpserver/Main.kt`

- [ ] **Step 1: Обновить Main.kt**

Открыть `mcp-server/src/main/kotlin/xyz/candycrawler/mcpserver/Main.kt`.

Добавить импорты:

```kotlin
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.util.getOrNull
import kotlinx.coroutines.withContext
import xyz.candycrawler.mcpserver.auth.UserRolesElement
import xyz.candycrawler.mcpserver.auth.UserRolesKey
```

В блоке `if (authIssuerUri != null && mcpBaseUrl != null)` после `install(McpAuthPlugin)` добавить interceptor:

```kotlin
                if (authIssuerUri != null && mcpBaseUrl != null) {
                    install(McpAuthPlugin) {
                        issuerUri = authIssuerUri
                        jwksUri = "$authIssuerUri/oauth2/jwks"
                        resourceMetadataUrl = "$mcpBaseUrl/.well-known/oauth-protected-resource"
                    }
                    // Inject roles from call.attributes (set by McpAuthPlugin) into coroutine context
                    // so tool handlers can access them via currentUserRoles()
                    intercept(ApplicationCallPipeline.Plugins) {
                        val roles = call.attributes.getOrNull(UserRolesKey) ?: emptyList()
                        withContext(UserRolesElement(roles)) {
                            proceed()
                        }
                    }
                    routing {
                        oauthMetadataRoutes(mcpBaseUrl, authIssuerUri)
                    }
                }
```

- [ ] **Step 2: Собрать проект**

```bash
./gradlew :mcp-server:build
```

Ожидаемый результат: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add mcp-server/src/main/kotlin/xyz/candycrawler/mcpserver/Main.kt
git commit -m "feat(mcp): inject user roles into coroutine context for tool handlers"
```

---

### Task 10: Проверка ролей в тул-хендлерах (McpServer.kt)

**Контекст:** FREE-тулы (`search_my_cards`, `search_scryfall`, `get_card`, `list_scryfall_format_codes`) доступны всем аутентифицированным пользователям. PRO-тулы требуют роли PRO или ADMIN. При stdio-транспорте auth отключён и `currentUserRoles()` вернёт пустой список — нужно добавить проверку, что stdio всегда пропускает (либо делать проверку только если roles claim присутствует).

**Решение:** При stdio-транспорте `UserRolesElement` в coroutineContext отсутствует → `currentUserRoles()` возвращает `emptyList()`. Чтобы не блокировать stdio, проверка активна только если в coroutineContext есть `UserRolesElement`. Добавим хелпер `isAuthEnabled()`.

**Files:**
- Modify: `mcp-server/src/main/kotlin/xyz/candycrawler/mcpserver/auth/UserRolesContext.kt`
- Modify: `mcp-server/src/main/kotlin/xyz/candycrawler/mcpserver/McpServer.kt`

- [ ] **Step 1: Добавить isAuthEnabled() в UserRolesContext.kt**

Открыть `mcp-server/src/main/kotlin/xyz/candycrawler/mcpserver/auth/UserRolesContext.kt`.

Добавить функцию в конец файла:

```kotlin
suspend fun isAuthEnabled(): Boolean =
    coroutineContext[UserRolesElement] != null
```

- [ ] **Step 2: Добавить хелпер requireRole в McpServer.kt**

Открыть `mcp-server/src/main/kotlin/xyz/candycrawler/mcpserver/McpServer.kt`.

Добавить импорты в начало `McpServer.kt`:

```kotlin
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import xyz.candycrawler.mcpserver.auth.ToolAccessConfig
import xyz.candycrawler.mcpserver.auth.currentUserRoles
import xyz.candycrawler.mcpserver.auth.isAuthEnabled
```

Добавить `private suspend fun checkAccess` в конец файла `McpServer.kt`, **вне** функции `createServer` (после закрывающей `}` `createServer`):

```kotlin
private suspend fun checkAccess(toolName: String): CallToolResult? {
    if (!isAuthEnabled()) return null
    val roles = currentUserRoles()
    if (ToolAccessConfig.hasAccess(toolName, roles)) return null
    return CallToolResult(
        content = listOf(TextContent("Access denied: tool '$toolName' requires PRO subscription")),
        isError = true,
    )
}
```

- [ ] **Step 3: Добавить проверку ролей для PRO-тулов**

В `McpServer.kt` обновить регистрацию пяти PRO-тулов, добавив вызов `checkAccess` в начале каждого хендлера:

Обновить все 5 PRO-тулов в `createServer`. Для каждого тула добавить вызов `checkAccess` в начале хендлера:

```kotlin
    server.addTool(
        name = "analyze_tribal_depth",
        description = "Analyze tribal depth for a given MTG creature type in your collection. Returns total card count, CMC distribution, role breakdown (creatures / kindred spells / tribal support cards), color spread, whether a lord or commander exists, and deck viability. Use this when the user asks about a specific tribe like Merfolk, Elf, Goblin, etc.",
        inputSchema = analyzeTribalDepthSchema(),
    ) { request ->
        checkAccess("analyze_tribal_depth")?.let { return@addTool it }
        handleAnalyzeTribalDepth(context, request)
    }
```

Аналогично обновить четыре оставшихся PRO-тула:

```kotlin
    server.addTool(
        name = "get_collection_overview",
        description = "Get a high-level summary of your entire card collection: total unique cards, breakdown by color (W/U/B/R/G/C), type (creature/instant/etc), rarity, and top 10 tribes with their colors. Use this when the user asks what their collection looks like or wants an overview before planning a deck.",
        inputSchema = getCollectionOverviewSchema(),
    ) { request ->
        checkAccess("get_collection_overview")?.let { return@addTool it }
        handleGetCollectionOverview(context, request)
    }

    server.addTool(
        name = "search_draftsim_articles",
        description = "Search favorited Draftsim.com articles about MTG draft strategy, set reviews, and limited format guides. Returns a lightweight list with id, title, slug and published date for browsing. Use get_draftsim_articles to fetch analyzed content for specific articles of interest.",
        inputSchema = searchDraftsimArticlesSchema(),
    ) { request ->
        checkAccess("search_draftsim_articles")?.let { return@addTool it }
        handleSearchDraftsimArticles(context, request)
    }

    server.addTool(
        name = "get_draftsim_articles",
        description = "Fetch analyzed MTG card knowledge from specific Draftsim articles by ID. Returns structured card evaluations (tiers, synergies, archetypes). Use after search_draftsim_articles to get content for articles of interest.",
        inputSchema = getDraftsimArticlesByIdSchema(),
    ) { request ->
        checkAccess("get_draftsim_articles")?.let { return@addTool it }
        handleGetDraftsimArticlesById(context, request)
    }

    server.addTool(
        name = "save_deck",
        description = """Save a finalized deck to your collection.
            IMPORTANT: Use search_my_cards first to find card IDs (the numeric 'id' field in results).
            Format rules: STANDARD requires mainboard >= 60 cards; SEALED and DRAFT require >= 40 cards.
            Max 4 copies of any single card.
            On success returns the saved deck ID.
            On validation failure (error response) the message explains what to fix — correct and retry.""",
        inputSchema = saveDeckSchema(),
    ) { request ->
        checkAccess("save_deck")?.let { return@addTool it }
        handleSaveDeck(context, request)
    }
```

FREE-тулы (`search_my_cards`, `search_scryfall`, `get_card`, `list_scryfall_format_codes`) остаются без изменений.

- [ ] **Step 4: Собрать проект**

```bash
./gradlew :mcp-server:build
```

Ожидаемый результат: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Запустить все тесты mcp-server**

```bash
./gradlew :mcp-server:test
```

Ожидаемый результат: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Запустить все тесты проекта**

```bash
./gradlew test
```

Ожидаемый результат: `BUILD SUCCESSFUL`, все тесты прошли.

- [ ] **Step 7: Commit**

```bash
git add mcp-server/src/main/kotlin/xyz/candycrawler/mcpserver/auth/UserRolesContext.kt
git add mcp-server/src/main/kotlin/xyz/candycrawler/mcpserver/McpServer.kt
git commit -m "feat(mcp): enforce role-based access control on PRO tools"
```

---

### Task 11: Динамическая фильтрация tools/list по ролям

**Контекст:** По умолчанию `Server.handleListTools()` возвращает все зарегистрированные тулы. Нам нужно, чтобы FREE-пользователь не видел PRO-тулы в списке вообще — это лучший UX (Claude не пытается вызывать тул, которого нет в его "зоне видимости").

**Решение:** Подкласс `FilteredMcpServer` переопределяет `createSession()` и устанавливает кастомный хендлер для `ListToolsRequest`, который фильтрует тулы через `ToolAccessConfig.hasAccess()` + `currentUserRoles()`. Роли уже инжектированы в coroutine context на уровне `Main.kt` (Task 9), поэтому `currentUserRoles()` корректно работает внутри хендлера. При stdio-транспорте `isAuthEnabled()` вернёт `false` — возвращаем все тулы без фильтрации.

**Files:**
- Create: `mcp-server/src/main/kotlin/xyz/candycrawler/mcpserver/FilteredMcpServer.kt`
- Modify: `mcp-server/src/main/kotlin/xyz/candycrawler/mcpserver/McpServer.kt` (заменить `Server(...)` на `FilteredMcpServer(...)`)
- Create: `mcp-server/src/test/kotlin/xyz/candycrawler/mcpserver/FilteredMcpServerTest.kt`

- [ ] **Step 1: Написать failing unit-тест**

Создать `mcp-server/src/test/kotlin/xyz/candycrawler/mcpserver/FilteredMcpServerTest.kt`:

```kotlin
package xyz.candycrawler.mcpserver

import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import xyz.candycrawler.mcpserver.auth.ToolAccessConfig
import xyz.candycrawler.mcpserver.auth.UserRolesElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FilteredMcpServerTest {

    private fun makeServer(): FilteredMcpServer {
        val server = FilteredMcpServer(
            serverInfo = Implementation(name = "test", version = "0.0.1"),
            options = ServerOptions(capabilities = ServerCapabilities(tools = ServerCapabilities.Tools(listChanged = true))),
        )
        // Register all 9 tools with stubs so we can verify filtering
        listOf(
            "search_my_cards", "search_scryfall", "get_card", "list_scryfall_format_codes",
            "analyze_tribal_depth", "get_collection_overview",
            "search_draftsim_articles", "get_draftsim_articles", "save_deck",
        ).forEach { name ->
            server.addTool(name = name, description = name) { _ ->
                error("stub")
            }
        }
        return server
    }

    @Test
    fun `FREE user sees only free tools`() = runBlocking {
        val server = makeServer()
        val roles = listOf("FREE")
        val visible = withContext(UserRolesElement(roles)) {
            server.visibleToolNames()
        }
        assertEquals(
            setOf("search_my_cards", "search_scryfall", "get_card", "list_scryfall_format_codes"),
            visible.toSet(),
        )
    }

    @Test
    fun `PRO user sees all tools`() = runBlocking {
        val server = makeServer()
        val roles = listOf("PRO")
        val visible = withContext(UserRolesElement(roles)) {
            server.visibleToolNames()
        }
        assertEquals(9, visible.size)
    }

    @Test
    fun `ADMIN user sees all tools`() = runBlocking {
        val server = makeServer()
        val roles = listOf("ADMIN")
        val visible = withContext(UserRolesElement(roles)) {
            server.visibleToolNames()
        }
        assertEquals(9, visible.size)
    }

    @Test
    fun `no auth context returns all tools (stdio mode)`() = runBlocking {
        val server = makeServer()
        // No UserRolesElement in context — simulates stdio transport
        val visible = server.visibleToolNames()
        assertEquals(9, visible.size)
    }
}
```

- [ ] **Step 2: Запустить тест — убедиться, что не компилируется**

```bash
./gradlew :mcp-server:test --tests "*.FilteredMcpServerTest" 2>&1 | head -20
```

Ожидаемый результат: ошибка компиляции `Unresolved reference: FilteredMcpServer`.

- [ ] **Step 3: Создать FilteredMcpServer**

Создать `mcp-server/src/main/kotlin/xyz/candycrawler/mcpserver/FilteredMcpServer.kt`:

```kotlin
package xyz.candycrawler.mcpserver

import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.ServerSession
import io.modelcontextprotocol.kotlin.sdk.shared.Transport
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ListToolsRequest
import io.modelcontextprotocol.kotlin.sdk.types.ListToolsResult
import io.modelcontextprotocol.kotlin.sdk.types.Method
import xyz.candycrawler.mcpserver.auth.ToolAccessConfig
import xyz.candycrawler.mcpserver.auth.currentUserRoles
import xyz.candycrawler.mcpserver.auth.isAuthEnabled

class FilteredMcpServer(
    serverInfo: Implementation,
    options: ServerOptions,
) : Server(serverInfo, options) {

    override suspend fun createSession(transport: Transport): ServerSession {
        val session = super.createSession(transport)
        session.setRequestHandler<ListToolsRequest>(Method.Defined.ToolsList) { _, _ ->
            ListToolsResult(tools = visibleToolNames().map { name -> tools[name]!!.tool }, nextCursor = null)
        }
        return session
    }

    suspend fun visibleToolNames(): List<String> {
        if (!isAuthEnabled()) return tools.keys.toList()
        val roles = currentUserRoles()
        return tools.keys.filter { toolName -> ToolAccessConfig.hasAccess(toolName, roles) }
    }
}
```

- [ ] **Step 4: Обновить McpServer.kt — использовать FilteredMcpServer**

В `McpServer.kt` заменить создание `Server(...)` на `FilteredMcpServer(...)`.

Добавить импорт:
```kotlin
import xyz.candycrawler.mcpserver.FilteredMcpServer
```

Заменить строки:
```kotlin
    val server = Server(
        serverInfo = Implementation(name = "mtg-bro", version = "1.0.0"),
        options = ServerOptions(
            capabilities = ServerCapabilities(tools = ServerCapabilities.Tools(listChanged = true)),
        ),
    )
```

на:
```kotlin
    val server = FilteredMcpServer(
        serverInfo = Implementation(name = "mtg-bro", version = "1.0.0"),
        options = ServerOptions(
            capabilities = ServerCapabilities(tools = ServerCapabilities.Tools(listChanged = true)),
        ),
    )
```

- [ ] **Step 5: Запустить тест**

```bash
./gradlew :mcp-server:test --tests "*.FilteredMcpServerTest"
```

Ожидаемый результат: `BUILD SUCCESSFUL`, 4 теста прошли.

- [ ] **Step 6: Запустить все тесты mcp-server**

```bash
./gradlew :mcp-server:test
```

Ожидаемый результат: `BUILD SUCCESSFUL`.

- [ ] **Step 7: Commit**

```bash
git add mcp-server/src/main/kotlin/xyz/candycrawler/mcpserver/FilteredMcpServer.kt
git add mcp-server/src/main/kotlin/xyz/candycrawler/mcpserver/McpServer.kt
git add mcp-server/src/test/kotlin/xyz/candycrawler/mcpserver/FilteredMcpServerTest.kt
git commit -m "feat(mcp): filter tools/list by user role via FilteredMcpServer"
```

---

## Финал: PR и проверка

- [ ] **Запустить полный билд**

```bash
./gradlew build
```

- [ ] **Проверить ручным тестом** (опционально, при наличии локальной среды)

Запустить `./gradlew :auth-service:bootRun` с профилем local.

Зарегистрировать пользователя:
```bash
curl -X POST http://localhost:8080/api/v1/users/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","username":"testuser","password":"password123"}'
```

Выполнить Authorization Code + PKCE flow (см. `auth-service/src/test/MANUAL_TESTING.md`).

Проверить, что полученный JWT содержит `"roles": ["FREE"]` claim (декодировать на jwt.io).

- [ ] **Создать PR**

```bash
git push origin feature/outh-public-client
gh pr create --title "feat: OAuth public client + RBAC (FREE/PRO/ADMIN roles)" \
  --body "$(cat <<'EOF'
## Summary
- Converts mcp-client OAuth client from confidential to public (no client secret required)
- Adds user_roles table with FREE/PRO/ADMIN roles; FREE assigned automatically on registration
- JWT access token now includes `roles` claim via OAuth2TokenCustomizer
- mcp-server enforces role-based access: FREE sees and can only call 4 basic tools; PRO/ADMIN see and can call all 9 tools
- Dynamic tools/list filtering via FilteredMcpServer — Claude never sees tools it can't use

## Test plan
- [ ] All unit and integration tests pass (`./gradlew test`)
- [ ] New user registration assigns FREE role (verify in DB)
- [ ] JWT from auth-service contains `roles: ["FREE"]` claim
- [ ] FREE user sees only 4 tools in tools/list response
- [ ] FREE user gets "Access denied" if somehow calling PRO tool directly
- [ ] PRO/ADMIN user sees all 9 tools
- [ ] stdio transport (Cursor) shows all tools without auth
EOF
)"
```
