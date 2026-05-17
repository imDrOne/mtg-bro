package xyz.candycrawler.draftsimparser.infrastructure.client.draftsim

import org.jsoup.Jsoup
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import xyz.candycrawler.draftsimparser.application.port.DraftsimArticleSearchResult
import xyz.candycrawler.draftsimparser.application.port.DraftsimArticleSource
import xyz.candycrawler.draftsimparser.application.port.DraftsimSourceArticle
import xyz.candycrawler.draftsimparser.infrastructure.client.draftsim.dto.WpPostResponse
import xyz.candycrawler.draftsimparser.infrastructure.client.draftsim.dto.WpTagResponse

@Component
class DraftsimWpApiClient(@Qualifier("draftsimRestClient") private val draftsimRestClient: RestClient) :
    DraftsimArticleSource {

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

    override fun findTagIdsBySetName(setName: String): List<Long> {
        val setSlug = slugify(setName)
        log.debug("Searching WP tags: setName={}, setSlug={}", setName, setSlug)

        val response = draftsimRestClient.get()
            .uri { builder ->
                builder.path("/wp-json/wp/v2/tags")
                    .queryParam("search", setSlug)
                    .queryParam("per_page", 100)
                    .build()
            }
            .retrieve()
            .body(object : ParameterizedTypeReference<List<WpTagResponse>>() {})
            .orEmpty()

        return filterTagsBySetSlug(response, setSlug)
    }

    override fun searchArticlesByTagIds(tagIds: List<Long>, page: Int, pageSize: Int): DraftsimArticleSearchResult {
        if (tagIds.isEmpty()) {
            return DraftsimArticleSearchResult(emptyList(), 0, 0)
        }

        log.debug("Fetching WP posts by tags: tagIds={}, page={}, pageSize={}", tagIds, page, pageSize)

        val tagsCsv = tagIds.joinToString(",")

        val response = draftsimRestClient.get()
            .uri { builder ->
                builder.path("/wp-json/wp/v2/posts")
                    .queryParam("tags", tagsCsv)
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

    companion object {
        internal fun slugify(name: String): String =
            name.lowercase()
                .replace(Regex("[^a-z0-9]+"), "-")
                .trim('-')

        internal fun filterTagsBySetSlug(tags: List<WpTagResponse>, setSlug: String): List<Long> {
            val requiredTokens = setSlug.split("-").filter { it.isNotBlank() }
            return tags
                .filter { tag ->
                    val tagTokens = tag.slug.split("-").toSet()
                    requiredTokens.all { it in tagTokens }
                }
                .map { it.id }
        }
    }
}
