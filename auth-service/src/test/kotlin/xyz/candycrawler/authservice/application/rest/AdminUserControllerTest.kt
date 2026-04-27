package xyz.candycrawler.authservice.application.rest

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import xyz.candycrawler.authservice.domain.user.exception.UserInvalidException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import xyz.candycrawler.authservice.application.service.AdminUserService
import xyz.candycrawler.authservice.domain.user.model.User
import xyz.candycrawler.authservice.domain.user.model.UserFilter
import xyz.candycrawler.authservice.lib.AbstractIntegrationTest
import xyz.candycrawler.common.pagination.PageResponse
import java.time.Instant

class AdminUserControllerTest : AbstractIntegrationTest() {

    @Autowired
    lateinit var wac: WebApplicationContext

    @MockitoBean
    lateinit var adminUserService: AdminUserService

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setup() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(wac)
            .apply<DefaultMockMvcBuilder>(SecurityMockMvcConfigurers.springSecurity())
            .build()
    }

    private fun user(id: Long, email: String) = User(
        id = id, email = email, username = "user$id",
        passwordHash = "\$2a\$10\$hash", enabled = true, createdAt = Instant.now(),
    )

    @Test
    @WithMockUser(authorities = ["PERM_api:admin:users:read"])
    fun `listUsers returns 200 with page response`() {
        val pageResponse = PageResponse(
            items = listOf(user(1L, "a@example.com")),
            page = 0, size = 20, totalItems = 1L, totalPages = 1,
        )
        whenever(adminUserService.listUsers(any<UserFilter>(), any())).thenReturn(pageResponse)

        mockMvc.get("/api/v1/admin/users") {
            param("page", "0")
            param("size", "20")
            param("sortBy", "createdAt")
            param("sortDir", "DESC")
        }.andExpect {
            status { isOk() }
            jsonPath("$.totalItems") { value(1) }
            jsonPath("$.items[0].email") { value("a@example.com") }
        }
    }

    @Test
    @WithMockUser(authorities = ["PERM_api:admin:users:read"])
    fun `listUsers returns 400 for invalid sortBy`() {
        mockMvc.get("/api/v1/admin/users") {
            param("sortBy", "invalidField")
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    @WithMockUser(authorities = ["PERM_api:admin:users:create"])
    fun `createAdminUser returns 201`() {
        whenever(adminUserService.createAdminUser(any(), any(), any())).thenReturn(user(1L, "admin@example.com"))

        mockMvc.post("/api/v1/admin/users") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email":"admin@example.com","username":"admin","password":"password123"}"""
        }.andExpect {
            status { isCreated() }
            jsonPath("$.email") { value("admin@example.com") }
        }
    }

    @Test
    @WithMockUser(username = "requester@example.com", authorities = ["PERM_api:admin:users:block"])
    fun `blockUser returns 204`() {
        mockMvc.post("/api/v1/admin/users/1/block").andExpect {
            status { isNoContent() }
        }
    }

    @Test
    @WithMockUser(username = "admin@example.com", authorities = ["PERM_api:admin:users:block"])
    fun `blockUser returns 422 when blocking self`() {
        whenever(adminUserService.blockUser(eq(1L), eq("admin@example.com")))
            .thenThrow(UserInvalidException("cannot block yourself"))

        mockMvc.post("/api/v1/admin/users/1/block").andExpect {
            status { isUnprocessableContent() }
        }
    }

    @Test
    @WithMockUser(authorities = ["PERM_api:admin:users:block"])
    fun `unblockUser returns 204`() {
        mockMvc.post("/api/v1/admin/users/1/unblock").andExpect {
            status { isNoContent() }
        }
    }

    @Test
    fun `listUsers returns 401 without authentication`() {
        mockMvc.get("/api/v1/admin/users").andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    @WithMockUser(authorities = ["PERM_api:admin:users:read"])
    fun `createAdminUser returns 403 without create permission`() {
        mockMvc.post("/api/v1/admin/users") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email":"admin@example.com","username":"admin","password":"password123"}"""
        }.andExpect {
            status { isForbidden() }
        }
    }
}
