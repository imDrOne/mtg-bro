package xyz.candycrawler.authservice.security

import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.source.ImmutableJWKSet
import com.nimbusds.jose.jwk.source.JWKSource
import com.nimbusds.jose.proc.SecurityContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.http.MediaType
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher
import xyz.candycrawler.authservice.domain.permission.repository.ApiPermissionRepository
import xyz.candycrawler.authservice.domain.user.model.UserRole
import xyz.candycrawler.authservice.domain.user.repository.UserRepository
import xyz.candycrawler.authservice.domain.user.repository.UserRoleRepository
import xyz.candycrawler.authservice.infrastructure.db.mapper.sql.RsaKeySqlMapper

@Configuration
class AuthorizationServerConfig(
    private val rsaKeySqlMapper: RsaKeySqlMapper,
    private val userRepository: UserRepository,
    private val userRoleRepository: UserRoleRepository,
    private val apiPermissionRepository: ApiPermissionRepository,
) {

    @Value("\${auth.issuer-uri}")
    private lateinit var issuerUri: String

    @Bean
    @Order(1)
    fun authorizationServerSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        val configurer = OAuth2AuthorizationServerConfigurer()
        http
            .securityMatcher(configurer.endpointsMatcher)
            .with(configurer) { authServer ->
                authServer
                    .authorizationServerMetadataEndpoint { metadataEndpoint ->
                        metadataEndpoint.authorizationServerMetadataCustomizer { builder ->
                            builder.clientRegistrationEndpoint("$issuerUri/connect/register")
                        }
                    }
                    .oidc { oidc ->
                        oidc.logoutEndpoint(Customizer.withDefaults())
                        oidc.providerConfigurationEndpoint { providerConfig ->
                            providerConfig.providerConfigurationCustomizer { builder ->
                                builder.clientRegistrationEndpoint("$issuerUri/connect/register")
                            }
                        }
                    }
            }
            .authorizeHttpRequests { it.anyRequest().authenticated() }
            .exceptionHandling { exceptions ->
                // Redirect browser requests (HTML) to the login page; JSON API clients get 401.
                exceptions.defaultAuthenticationEntryPointFor(
                    LoginUrlAuthenticationEntryPoint("/login"),
                    MediaTypeRequestMatcher(MediaType.TEXT_HTML),
                )
            }
        return http.build()
    }

    @Bean
    fun registeredClientRepository(jdbcTemplate: JdbcTemplate): RegisteredClientRepository =
        JdbcRegisteredClientRepository(jdbcTemplate)

    @Bean
    fun authorizationService(
        jdbcTemplate: JdbcTemplate,
        registeredClientRepository: RegisteredClientRepository,
    ) = JdbcOAuth2AuthorizationService(jdbcTemplate, registeredClientRepository)

    @Bean
    fun authorizationConsentService(
        jdbcTemplate: JdbcTemplate,
        registeredClientRepository: RegisteredClientRepository,
    ) = JdbcOAuth2AuthorizationConsentService(jdbcTemplate, registeredClientRepository)

    @Bean
    fun authorizationServerSettings(): AuthorizationServerSettings =
        AuthorizationServerSettings.builder()
            .issuer(issuerUri)
            .build()

    @Bean
    fun jwkSource(): JWKSource<SecurityContext> {
        val rsaKey = rsaKeySqlMapper.loadOrGenerate()
        return ImmutableJWKSet(JWKSet(rsaKey))
    }

    @Bean
    fun jwtDecoder(jwkSource: JWKSource<SecurityContext>): JwtDecoder =
        OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource)

    @Bean
    fun jwtEncoder(jwkSource: JWKSource<SecurityContext>): JwtEncoder =
        NimbusJwtEncoder(jwkSource)

    @Bean
    fun jwtCustomizer(): OAuth2TokenCustomizer<JwtEncodingContext> =
        OAuth2TokenCustomizer { context ->
            if (context.tokenType == OAuth2TokenType.ACCESS_TOKEN) {
                val email = context.getPrincipal<Authentication>().name
                val user = userRepository.findByEmail(email) ?: return@OAuth2TokenCustomizer
                val roles = userRoleRepository.findByUserId(user.id!!)
                context.claims.claim("user_id", user.id)
                context.claims.claim("roles", roles.map { it.name })
                val permissions = apiPermissionRepository.findByRoles(roles)
                context.claims.claim("permissions", permissions.map { it.name })
            }
        }
}
