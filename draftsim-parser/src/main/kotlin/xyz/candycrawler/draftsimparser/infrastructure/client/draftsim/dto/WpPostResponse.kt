package xyz.candycrawler.draftsimparser.infrastructure.client.draftsim.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

data class WpPostResponse(
    val id: Long,
    val date: LocalDateTime,
    val slug: String,
    val link: String,
    val title: WpRendered,
    val content: WpRendered,
) {
    data class WpRendered(
        @JsonProperty("rendered")
        val rendered: String,
    )
}
