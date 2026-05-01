package xyz.candycrawler.draftsimparser.infrastructure.vector

import org.springframework.ai.document.Document
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.stereotype.Component
import xyz.candycrawler.draftsimparser.application.port.ArticleVectorDocument
import xyz.candycrawler.draftsimparser.application.port.ArticleVectorSearchMatch
import xyz.candycrawler.draftsimparser.application.port.ArticleVectorStore

@ConditionalOnBean(VectorStore::class)
@Component
class SpringAiArticleVectorStore(
    private val vectorStore: VectorStore,
) : ArticleVectorStore {

    override fun replaceArticleDocuments(articleId: Long, documents: List<ArticleVectorDocument>) {
        val filter = FilterExpressionBuilder().eq(ARTICLE_ID, articleId).build()
        vectorStore.delete(filter)

        if (documents.isNotEmpty()) {
            vectorStore.add(
                documents.map { document ->
                    Document.builder()
                        .id(document.id)
                        .text(document.content)
                        .metadata(document.metadata)
                        .build()
                }
            )
        }
    }

    override fun search(query: String, topK: Int, similarityThreshold: Double): List<ArticleVectorSearchMatch> =
        vectorStore.similaritySearch(
            SearchRequest.builder()
                .query(query)
                .topK(topK)
                .similarityThreshold(similarityThreshold)
                .build()
        ).orEmpty().mapNotNull { document ->
            val articleId = document.metadata[ARTICLE_ID].toLongOrNull() ?: return@mapNotNull null
            ArticleVectorSearchMatch(
                articleId = articleId,
                score = document.score,
                content = document.text.orEmpty(),
                metadata = document.metadata,
            )
        }

    companion object {
        const val ARTICLE_ID = "article_id"
    }
}

private fun Any?.toLongOrNull(): Long? =
    when (this) {
        is Number -> this.toLong()
        is String -> this.toLongOrNull()
        else -> null
    }
