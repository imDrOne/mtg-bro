package xyz.candycrawler.wizardstataggregator.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain = http
        .csrf { it.disable() }
        .authorizeHttpRequests { auth ->
            auth
                .requestMatchers(
                    "/actuator/health", "/actuator/info",
                    "/swagger-ui.html", "/swagger-ui/**", "/webjars/**",
                    "/api-docs", "/api-docs/**"
                ).permitAll()
                .anyRequest().authenticated()
        }
        .oauth2ResourceServer { it.jwt { jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()) } }
        .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
        .build()

    private fun jwtAuthenticationConverter(): JwtAuthenticationConverter {
        val converter = JwtAuthenticationConverter()
        converter.setJwtGrantedAuthoritiesConverter { jwt ->
            val permissions = jwt.getClaimAsStringList("permissions").orEmpty()
                .map { SimpleGrantedAuthority("PERM_$it") }
            val roles = jwt.getClaimAsStringList("roles").orEmpty()
                .map { SimpleGrantedAuthority("ROLE_$it") }
            permissions + roles
        }
        return converter
    }
}
