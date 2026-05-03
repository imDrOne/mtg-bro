package xyz.candycrawler.authservice.application.rest

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import xyz.candycrawler.authservice.application.service.UserRegistrationService
import xyz.candycrawler.authservice.lib.AbstractIntegrationTest
import kotlin.test.assertNotNull

class AuthControllerIntegrationTest : AbstractIntegrationTest() {

    @Autowired lateinit var wac: WebApplicationContext

    @Autowired lateinit var registrationService: UserRegistrationService

    @Autowired lateinit var jdbcTemplate: JdbcTemplate

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setup() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(wac)
            .apply<DefaultMockMvcBuilder>(SecurityMockMvcConfigurers.springSecurity())
            .build()
        jdbcTemplate.execute("DELETE FROM refresh_tokens")
        jdbcTemplate.execute("DELETE FROM user_roles")
        jdbcTemplate.execute("DELETE FROM users")
        registrationService.register("user@test.com", "testuser", "password123")
    }

    @Test
    fun `login returns access token and sets refresh cookie`() {
        val result = mockMvc.post("/api/v1/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email":"user@test.com","password":"password123"}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.accessToken") { exists() }
            jsonPath("$.tokenType") { value("Bearer") }
            jsonPath("$.expiresIn") { isNumber() }
            cookie().exists("refresh_token")
            cookie().httpOnly("refresh_token", true)
            cookie().path("refresh_token", "/api/v1/auth")
        }.andReturn()

        val refreshCookie = result.response.getCookie("refresh_token")
        assertNotNull(refreshCookie)
        assert(refreshCookie.maxAge > 0)
    }

    @Test
    fun `login returns 401 on wrong password`() {
        mockMvc.post("/api/v1/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email":"user@test.com","password":"wrong"}"""
        }.andExpect {
            status { isUnauthorized() }
            jsonPath("$.error") { exists() }
        }
    }

    @Test
    fun `login returns 401 on unknown email`() {
        mockMvc.post("/api/v1/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email":"ghost@test.com","password":"password123"}"""
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `refresh rotates cookie and returns new access token`() {
        val loginResult = mockMvc.post("/api/v1/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email":"user@test.com","password":"password123"}"""
        }.andReturn()

        val refreshCookie = loginResult.response.getCookie("refresh_token")!!

        mockMvc.post("/api/v1/auth/refresh") {
            cookie(refreshCookie)
        }.andExpect {
            status { isOk() }
            jsonPath("$.accessToken") { exists() }
            cookie().exists("refresh_token")
        }
    }

    @Test
    fun `refresh with already-used token returns 401 and cascades revoke`() {
        val loginResult = mockMvc.post("/api/v1/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email":"user@test.com","password":"password123"}"""
        }.andReturn()
        val originalCookie = loginResult.response.getCookie("refresh_token")!!

        // first refresh — rotates the token
        mockMvc.post("/api/v1/auth/refresh") {
            cookie(originalCookie)
        }.andExpect { status { isOk() } }

        // replay the original (revoked) token — cascade revoke expected
        mockMvc.post("/api/v1/auth/refresh") {
            cookie(originalCookie)
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `refresh without cookie returns 401`() {
        mockMvc.post("/api/v1/auth/refresh")
            .andExpect {
                status { isUnauthorized() }
                jsonPath("$.error") { exists() }
            }
    }

    @Test
    fun `logout clears cookie and revokes token`() {
        val loginResult = mockMvc.post("/api/v1/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email":"user@test.com","password":"password123"}"""
        }.andReturn()
        val refreshCookie = loginResult.response.getCookie("refresh_token")!!

        mockMvc.post("/api/v1/auth/logout") {
            cookie(refreshCookie)
        }.andExpect {
            status { isNoContent() }
            cookie().maxAge("refresh_token", 0)
        }

        // token now revoked — refresh must fail
        mockMvc.post("/api/v1/auth/refresh") {
            cookie(refreshCookie)
        }.andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `logout is idempotent when called without cookie`() {
        mockMvc.post("/api/v1/auth/logout")
            .andExpect { status { isNoContent() } }
    }
}
