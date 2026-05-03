package xyz.candycrawler.authservice.infrastructure.security

import com.github.benmanes.caffeine.cache.Caffeine
import io.github.bucket4j.Bandwidth
import io.github.bucket4j.BucketConfiguration
import io.github.bucket4j.caffeine.Bucket4jCaffeine
import io.github.bucket4j.caffeine.CaffeineProxyManager
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor
import tools.jackson.databind.ObjectMapper
import java.time.Duration

@Component
class RateLimitingInterceptor(
    private val objectMapper: ObjectMapper,
    private val trustedProxyValidator: TrustedProxyValidator,
) : HandlerInterceptor {

    private val registrationProxyManager: CaffeineProxyManager<String> =
        Bucket4jCaffeine.builderFor<String>(
            Caffeine.newBuilder()
                .maximumSize(100_000),
        ).build()

    private val loginProxyManager: CaffeineProxyManager<String> =
        Bucket4jCaffeine.builderFor<String>(
            Caffeine.newBuilder()
                .maximumSize(100_000),
        ).build()

    private val dcrProxyManager: CaffeineProxyManager<String> =
        Bucket4jCaffeine.builderFor<String>(
            Caffeine.newBuilder()
                .maximumSize(100_000),
        ).build()

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val ip = resolveClientIp(request)
        val path = request.requestURI

        val bucket = when {
            path.startsWith("/api/v1/users/register") ->
                registrationProxyManager.builder().build(ip) { registrationConfig() }

            path == "/login" ->
                loginProxyManager.builder().build(ip) { loginConfig() }

            path == "/connect/register" ->
                dcrProxyManager.builder().build(ip) { dcrConfig() }

            else -> null
        }

        return when {
            bucket == null -> true

            bucket.tryConsume(1) -> true

            else -> {
                response.status = HttpStatus.TOO_MANY_REQUESTS.value()
                response.contentType = MediaType.APPLICATION_JSON_VALUE
                objectMapper.writeValue(response.writer, mapOf("error" to "Too many requests"))
                false
            }
        }
    }

    private fun registrationConfig(): BucketConfiguration = BucketConfiguration.builder()
        .addLimit(
            Bandwidth.builder()
                .capacity(5)
                .refillIntervally(5, Duration.ofMinutes(10))
                .build(),
        )
        .build()

    private fun loginConfig(): BucketConfiguration = BucketConfiguration.builder()
        .addLimit(
            Bandwidth.builder()
                .capacity(10)
                .refillIntervally(10, Duration.ofMinutes(5))
                .build(),
        )
        .build()

    private fun dcrConfig(): BucketConfiguration = BucketConfiguration.builder()
        .addLimit(
            Bandwidth.builder()
                .capacity(10)
                .refillIntervally(10, Duration.ofMinutes(10))
                .build(),
        )
        .build()

    private fun resolveClientIp(request: HttpServletRequest): String {
        val xff = request.getHeader("X-Forwarded-For")
        return if (xff != null && trustedProxyValidator.isTrusted(request.remoteAddr)) {
            xff.split(",").firstOrNull()?.trim() ?: request.remoteAddr
        } else {
            request.remoteAddr
        }
    }
}
