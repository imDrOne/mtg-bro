package xyz.candycrawler.authservice.infrastructure.security

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebMvcConfig(private val rateLimitingInterceptor: RateLimitingInterceptor) : WebMvcConfigurer {

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(rateLimitingInterceptor)
            .addPathPatterns("/api/v1/users/register", "/login", "/connect/register")
    }
}
