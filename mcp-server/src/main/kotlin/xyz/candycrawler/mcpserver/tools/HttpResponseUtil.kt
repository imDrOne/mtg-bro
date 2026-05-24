package xyz.candycrawler.mcpserver.tools

import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess

internal const val MAX_ERROR_BODY_PREVIEW = 300

internal class DownstreamHttpException(
    val status: Int,
    val statusText: String,
    val bodyPreview: String,
    endpointLabel: String,
) : RuntimeException(
    "Downstream $endpointLabel returned HTTP $status $statusText. Body: ${bodyPreview.ifBlank { "<empty>" }}",
)

internal class DownstreamUnauthorizedException(endpointLabel: String) :
    Exception("Downstream service returned 401 Unauthorized from $endpointLabel")

internal suspend fun HttpResponse.readTextOrFail(endpointLabel: String): String {
    val text = bodyAsText()
    if (status == HttpStatusCode.Unauthorized) throw DownstreamUnauthorizedException(endpointLabel)
    if (!status.isSuccess()) {
        throw DownstreamHttpException(
            status = status.value,
            statusText = status.description,
            bodyPreview = text.take(MAX_ERROR_BODY_PREVIEW),
            endpointLabel = endpointLabel,
        )
    }
    return text
}
