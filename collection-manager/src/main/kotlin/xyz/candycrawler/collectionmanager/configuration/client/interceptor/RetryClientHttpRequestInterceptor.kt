package xyz.candycrawler.collectionmanager.configuration.client.interceptor

import org.slf4j.LoggerFactory
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.web.client.RestClientException
import java.io.IOException

class RetryClientHttpRequestInterceptor(
    private val maxAttempts: Int,
    private val initialDelayMs: Long,
    private val multiplier: Double,
    private val maxDelayMs: Long,
) : ClientHttpRequestInterceptor {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution,
    ): ClientHttpResponse {
        var delayMs = initialDelayMs
        var lastException: IOException? = null

        for (attempt in 1..maxAttempts) {
            try {
                val response = execution.execute(request, body)

                if (response.statusCode.is5xxServerError && attempt < maxAttempts) {
                    response.close()
                    log.warn(
                        "Attempt {}/{}: {} {} returned {}, retrying in {}ms",
                        attempt,
                        maxAttempts,
                        request.method,
                        request.uri,
                        response.statusCode,
                        delayMs,
                    )
                    Thread.sleep(delayMs)
                    delayMs = (delayMs * multiplier).toLong().coerceAtMost(maxDelayMs)
                    continue
                }

                return response
            } catch (e: IOException) {
                lastException = e
                if (attempt < maxAttempts) {
                    log.warn(
                        "Attempt {}/{}: {} {} failed with '{}', retrying in {}ms",
                        attempt,
                        maxAttempts,
                        request.method,
                        request.uri,
                        e.message,
                        delayMs,
                    )
                    Thread.sleep(delayMs)
                    delayMs = (delayMs * multiplier).toLong().coerceAtMost(maxDelayMs)
                }
            }
        }

        val message = "All $maxAttempts attempts failed for ${request.method} ${request.uri}"
        log.error(message)
        throw lastException ?: RestClientException(message)
    }
}
