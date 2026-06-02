package xyz.candycrawler.draftsimparser.infrastructure.vector

import org.springframework.ai.document.Document
import org.springframework.ai.embedding.AbstractEmbeddingModel
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.ai.embedding.EmbeddingRequest
import org.springframework.ai.embedding.EmbeddingResponse
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

// Satisfies QdrantVectorStoreAutoConfiguration's EmbeddingModel requirement when vector indexing
// is disabled (ARTICLE_VECTOR_INDEX_ENABLED=false) and no real embedding provider is configured.
// Spring AI's Qdrant auto-config has matchIfMissing=true, so it always runs on this classpath;
// this bean lets it succeed without an API key while SpringAiArticleVectorStore stays dormant.
@ConditionalOnProperty(
    prefix = "infrastructure.vector-index",
    name = ["enabled"],
    havingValue = "false",
)
@ConditionalOnMissingBean(EmbeddingModel::class)
@Component
class NoOpEmbeddingModel : AbstractEmbeddingModel() {
    override fun call(request: EmbeddingRequest): EmbeddingResponse = EmbeddingResponse(emptyList())
    override fun embed(document: Document): FloatArray = FloatArray(0)
}
