package xyz.candycrawler.collectionmanager.configuration.client.interceptor

import org.slf4j.LoggerFactory
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse

class LoggingClientHttpRequestInterceptor : ClientHttpRequestInterceptor {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution,
    ): ClientHttpResponse {
        log.debug(">>> {} {}", request.method, request.uri)
        val response = execution.execute(request, body)
        log.debug("<<< {} {} -> {}", request.method, request.uri, response.statusCode)
        return response
    }
}
