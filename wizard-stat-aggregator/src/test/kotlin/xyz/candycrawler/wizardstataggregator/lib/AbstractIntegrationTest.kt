package xyz.candycrawler.wizardstataggregator.lib

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.postgresql.PostgreSQLContainer
import xyz.candycrawler.wizardstataggregator.infrastructure.client.lands17.Lands17ApiClient

@SpringBootTest
@ActiveProfiles("test")
abstract class AbstractIntegrationTest {

    protected val logger: Logger = LoggerFactory.getLogger(Lands17ApiClient::class.java)

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
        }
    }
}
