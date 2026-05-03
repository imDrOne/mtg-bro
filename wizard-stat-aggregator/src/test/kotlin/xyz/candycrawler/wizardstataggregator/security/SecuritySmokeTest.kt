package xyz.candycrawler.wizardstataggregator.security

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import xyz.candycrawler.wizardstataggregator.application.service.CardLimitedStatsCollectionService
import xyz.candycrawler.wizardstataggregator.application.service.TrackedLimitedStatSetService
import xyz.candycrawler.wizardstataggregator.lib.AbstractIntegrationTest

class SecuritySmokeTest : AbstractIntegrationTest() {

    @Autowired lateinit var wac: WebApplicationContext

    @MockitoBean
    lateinit var collectionService: CardLimitedStatsCollectionService

    @MockitoBean
    lateinit var trackedSetService: TrackedLimitedStatSetService

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
    fun `POST collect with JWT without admin role returns 403`() {
        mockMvc.post("/api/v1/card-limited-stats/collect") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"setCode":"BLB"}"""
            with(jwt().authorities(SimpleGrantedAuthority("PERM_api:stats:collect")))
        }.andExpect { status { isForbidden() } }
    }

    @Test
    fun `GET tracked sets with admin role but without permission returns 403`() {
        mockMvc.get("/api/v1/card-limited-stats/tracked-sets") {
            with(jwt().authorities(SimpleGrantedAuthority("ROLE_ADMIN")))
        }.andExpect { status { isForbidden() } }
    }

    @Test
    fun `GET tracked sets with admin role and permission returns 200`() {
        mockMvc.get("/api/v1/card-limited-stats/tracked-sets") {
            with(
                jwt().authorities(
                    SimpleGrantedAuthority("ROLE_ADMIN"),
                    SimpleGrantedAuthority("PERM_api:stats:tracked-sets:manage"),
                ),
            )
        }.andExpect { status { isOk() } }
    }
}
