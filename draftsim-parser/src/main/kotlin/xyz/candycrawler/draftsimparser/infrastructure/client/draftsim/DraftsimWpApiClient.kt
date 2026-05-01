package xyz.candycrawler.draftsimparser.infrastructure.client.draftsim

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.jsoup.Jsoup
import xyz.candycrawler.draftsimparser.application.port.DraftsimArticleSearchResult
import xyz.candycrawler.draftsimparser.application.port.DraftsimArticleSource
import xyz.candycrawler.draftsimparser.application.port.DraftsimSourceArticle
import xyz.candycrawler.draftsimparser.infrastructure.client.draftsim.dto.WpPostResponse

@Component
class DraftsimWpApiClient(
    @Qualifier("draftsimRestClient") private val draftsimRestClient: RestClient,
) : DraftsimArticleSource {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun searchArticles(keyword: String, page: Int, pageSize: Int): DraftsimArticleSearchResult {
        log.debug("Fetching WP posts: keyword={}, page={}, pageSize={}", keyword, page, pageSize)

        val response = draftsimRestClient.get()
            .uri { builder ->
                builder.path("/wp-json/wp/v2/posts")
                    .queryParam("search", keyword)
                    .queryParam("per_page", pageSize)
                    .queryParam("page", page)
                    .build()
            }
            .retrieve()
            .toEntity(object : ParameterizedTypeReference<List<WpPostResponse>>() {})

        val totalPages = response.headers.getFirst("X-WP-TotalPages")?.toIntOrNull() ?: 1
        val totalPosts = response.headers.getFirst("X-WP-Total")?.toIntOrNull() ?: 0

        return DraftsimArticleSearchResult(
            articles = response.body.orEmpty().map { it.toSourceArticle() },
            totalPages = totalPages,
            totalArticles = totalPosts,
        )
    }

    private fun WpPostResponse.toSourceArticle(): DraftsimSourceArticle {
        val textContent = Jsoup.parse(content.rendered)
            .select("p")
            .map { it.text() }
            .filter { it.isNotBlank() }
            .joinToString("\n\n")

        return DraftsimSourceArticle(
            externalId = id,
            title = title.rendered,
            slug = slug,
            url = link,
            htmlContent = content.rendered,
            textContent = textContent,
            publishedAt = date,
        )
    }
}
