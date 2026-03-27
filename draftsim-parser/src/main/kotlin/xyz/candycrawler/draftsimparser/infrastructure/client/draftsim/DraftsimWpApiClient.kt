package xyz.candycrawler.draftsimparser.infrastructure.client.draftsim

import org.slf4j.LoggerFactory
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import xyz.candycrawler.draftsimparser.infrastructure.client.draftsim.dto.WpPostResponse

@Component
class DraftsimWpApiClient(private val draftsimRestClient: RestClient) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun searchPosts(keyword: String, page: Int, perPage: Int = 10): WpSearchResult {
        log.debug("Fetching WP posts: keyword={}, page={}, perPage={}", keyword, page, perPage)

        val response = draftsimRestClient.get()
            .uri { builder ->
                builder.path("/wp-json/wp/v2/posts")
                    .queryParam("search", keyword)
                    .queryParam("per_page", perPage)
                    .queryParam("page", page)
                    .build()
            }
            .retrieve()
            .toEntity(object : ParameterizedTypeReference<List<WpPostResponse>>() {})

        val totalPages = response.headers.getFirst("X-WP-TotalPages")?.toIntOrNull() ?: 1
        val totalPosts = response.headers.getFirst("X-WP-Total")?.toIntOrNull() ?: 0

        return WpSearchResult(
            posts = response.body ?: emptyList(),
            totalPages = totalPages,
            totalPosts = totalPosts,
        )
    }

    data class WpSearchResult(
        val posts: List<WpPostResponse>,
        val totalPages: Int,
        val totalPosts: Int,
    )
}
