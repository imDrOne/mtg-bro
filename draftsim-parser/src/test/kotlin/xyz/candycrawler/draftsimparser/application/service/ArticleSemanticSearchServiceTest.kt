package xyz.candycrawler.draftsimparser.application.service

import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.support.DefaultListableBeanFactory
import xyz.candycrawler.draftsimparser.application.port.ArticleVectorDocument
import xyz.candycrawler.draftsimparser.application.port.ArticleVectorSearchMatch
import xyz.candycrawler.draftsimparser.application.port.ArticleVectorStore
import xyz.candycrawler.draftsimparser.domain.article.model.Article
import xyz.candycrawler.draftsimparser.domain.article.repository.QueryArticleRepository
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class ArticleSemanticSearchServiceTest {

    private val vectorStore = RecordingArticleVectorStore()
    private val beanFactory = DefaultListableBeanFactory().also {
        it.registerSingleton("articleVectorStore", vectorStore)
    }
    private val queryArticleRepository = mock<QueryArticleRepository>()
    private val service = ArticleSemanticSearchService(
        vectorStoreProvider = beanFactory.getBeanProvider(ArticleVectorStore::class.java),
        queryArticleRepository = queryArticleRepository,
        enabled = true,
        defaultTopK = 8,
        defaultSimilarityThreshold = 0.65,
    )

    @Test
    fun `search groups vector matches by current favorite articles`() {
        vectorStore.matches = listOf(
            ArticleVectorSearchMatch(
                articleId = 1,
                score = 0.91,
                content = "Station rewards tapping creatures.",
                metadata = mapOf("insight_type" to "mechanic", "subject" to "Station", "tags" to listOf("mechanic")),
            ),
            ArticleVectorSearchMatch(
                articleId = 2,
                score = 0.89,
                content = "Ignored because article is not favorited.",
                metadata = emptyMap(),
            ),
        )
        whenever(queryArticleRepository.findById(1)).thenReturn(article(1, favorite = true))
        whenever(queryArticleRepository.findById(2)).thenReturn(article(2, favorite = false))

        val results = service.search("tap mechanic")

        assertEquals(1, results.size)
        assertEquals(1, results.single().article.id)
        assertEquals("Station", results.single().matches.single().subject)
        assertEquals("tap mechanic", vectorStore.query)
        assertEquals(8, vectorStore.topK)
        assertEquals(0.65, vectorStore.similarityThreshold)
    }

    private fun article(id: Long, favorite: Boolean) = Article(
        id = id,
        externalId = id,
        title = "Article $id",
        slug = "article-$id",
        url = "https://draftsim.com/article-$id",
        htmlContent = null,
        textContent = null,
        analyzedText = null,
        keywords = emptyList(),
        favorite = favorite,
        errorMsg = null,
        analyzStartedAt = null,
        analyzEndedAt = null,
        publishedAt = LocalDateTime.parse("2026-01-01T00:00:00"),
        fetchedAt = LocalDateTime.parse("2026-01-01T00:00:00"),
    )

    private class RecordingArticleVectorStore : ArticleVectorStore {
        var matches: List<ArticleVectorSearchMatch> = emptyList()
        var query: String? = null
        var topK: Int? = null
        var similarityThreshold: Double? = null

        override fun replaceArticleDocuments(articleId: Long, documents: List<ArticleVectorDocument>) = Unit

        override fun search(query: String, topK: Int, similarityThreshold: Double): List<ArticleVectorSearchMatch> {
            this.query = query
            this.topK = topK
            this.similarityThreshold = similarityThreshold
            return matches
        }
    }
}
