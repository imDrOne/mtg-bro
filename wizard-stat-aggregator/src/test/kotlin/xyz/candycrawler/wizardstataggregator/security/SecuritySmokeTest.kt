package xyz.candycrawler.wizardstataggregator.security

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import xyz.candycrawler.wizardstataggregator.lib.AbstractIntegrationTest

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
    fun `POST collect without token returns 401`() {
        mockMvc.post("/api/v1/card-limited-stats/collect") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"setCode":"BLB"}"""
        }.andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `POST collect with valid JWT passes authentication`() {
        val status = mockMvc.post("/api/v1/card-limited-stats/collect") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"setCode":"BLB"}"""
            with(jwt())
        }.andReturn().response.status
        assert(status != 401) { "Expected non-401 with JWT, got $status" }
    }
}
