package xyz.candycrawler.collectionmanager.security

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import xyz.candycrawler.collectionmanager.lib.AbstractIntegrationTest

class SecuritySmokeTest : AbstractIntegrationTest() {

    @Autowired lateinit var wac: WebApplicationContext
    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setup() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(wac)
            .apply<DefaultMockMvcBuilder>(SecurityMockMvcConfigurers.springSecurity())
            .build()
    }

    @Test
    fun `GET cards search without token returns 401`() {
        mockMvc.get("/api/v1/cards/search").andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `GET cards search with valid JWT passes authentication`() {
        val status = mockMvc.get("/api/v1/cards/search") {
            with(jwt())
        }.andReturn().response.status
        assert(status != 401) { "Expected non-401 with JWT, got $status" }
    }
}
