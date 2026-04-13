package xyz.candycrawler.authservice.lib

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.postgresql.PostgreSQLContainer

@SpringBootTest
@ActiveProfiles("test")
abstract class AbstractIntegrationTest {

    companion object {
        private val postgres = PostgreSQLContainer("postgres:16-alpine")

        init {
            postgres.start()
        }

        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            registry.add("auth.issuer-uri") { "http://localhost:8080" }
            registry.add("auth.mcp-client.redirect-uri") { "http://localhost:3000/oauth2/callback" }
            registry.add("auth.trusted-proxy-cidr") { "127.0.0.1/32" }
        }
    }
}
