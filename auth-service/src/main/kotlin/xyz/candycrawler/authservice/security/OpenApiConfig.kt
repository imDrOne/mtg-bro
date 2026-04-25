package xyz.candycrawler.authservice.security

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun openApi(): OpenAPI = OpenAPI()
        .info(
            Info()
                .title("Auth Service API")
                .description("Authentication and token management. Use POST /api/v1/auth/login to obtain a JWT.")
                .version("1.0.0")
        )
        .components(
            Components()
                .addSecuritySchemes(
                    "bearer-jwt",
                    SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT access token from POST /api/v1/auth/login")
                )
                .addSecuritySchemes(
                    "refresh-cookie",
                    SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .`in`(SecurityScheme.In.COOKIE)
                        .name("refresh_token")
                        .description("Refresh token cookie — set automatically on login, used by POST /api/v1/auth/refresh and /logout")
                )
        )
}
