package xyz.candycrawler.draftsimparser.application.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import xyz.candycrawler.draftsimparser.application.port.ArticleVectorDocument
import xyz.candycrawler.draftsimparser.application.port.ArticleVectorStore
import xyz.candycrawler.draftsimparser.domain.article.model.Article
import java.nio.charset.StandardCharsets
import java.util.UUID

@Service
class ArticleVectorIndexService(
    private val vectorStoreProvider: ObjectProvider<ArticleVectorStore>,
    private val objectMapper: ObjectMapper,
    @Value("\${infrastructure.vector-index.enabled}") private val enabled: Boolean,
) : DisposableBean {

    private val log = LoggerFactory.getLogger(javaClass)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val semaphore = Semaphore(MAX_CONCURRENT_VECTOR_INDEXES)

    fun replaceIndex(article: Article) {
        val articleId = article.id
        if (enabled && articleId != null) {
            val vectorStore = vectorStoreProvider.getIfAvailable()
            if (vectorStore == null) {
                log.info("Article id={}: vector index is enabled but no ArticleVectorStore is available", articleId)
            } else {
                runCatching {
                    vectorStore.replaceArticleDocuments(articleId, buildDocuments(article))
                }.onFailure { ex ->
                    log.error("Article id={}: vector indexing failed", articleId, ex)
                }
            }
        }
    }

    fun replaceIndexesAsync(articles: List<Article>) {
        if (!enabled) return
        val uniqueArticles = articles.distinctBy { it.id }
        if (uniqueArticles.isEmpty()) return

        scope.launch {
            coroutineScope {
                uniqueArticles.map { article ->
                    async {
                        semaphore.withPermit {
                            replaceIndex(article)
                        }
                    }
                }.awaitAll()
            }
        }
    }

    internal fun buildDocuments(article: Article): List<ArticleVectorDocument> {
        val articleId = article.id
        val analyzedText = article.analyzedText
        val root = analyzedText?.let { runCatching { objectMapper.readTree(it) }.getOrNull() }
        val insights = root
            ?.takeIf { it["schema_version"]?.asInt() == ARTICLE_SCHEMA_VERSION }
            ?.get("insights")
            ?.takeIf { it.isArray }

        return if (articleId == null || insights == null) {
            emptyList()
        } else {
            insights.mapIndexedNotNull { index, insight ->
                val content = insight
                    .takeIf { it.isObject }
                    ?.let { buildContent(article, it) }
                    ?.takeIf { it.isNotBlank() }

                content?.let {
                    ArticleVectorDocument(
                        id = buildDocumentId(articleId, index),
                        content = it,
                        metadata = buildMetadata(article, root, insight, index),
                    )
                }
            }
        }
    }

    private fun buildContent(article: Article, insight: JsonNode): String = listOfNotNull(
        "Article: ${article.title}",
        insight["subject"]?.asString()?.takeIf { it.isNotBlank() }?.let { "Subject: $it" },
        insight["type"]?.asString()?.takeIf { it.isNotBlank() }?.let { "Type: $it" },
        insight["summary"]?.asString()?.takeIf { it.isNotBlank() },
        insight["deckbuilding_implications"]?.stringValues()?.takeIf { it.isNotEmpty() }
            ?.joinToString(prefix = "Deckbuilding implications: "),
        insight["related_cards"]?.stringValues()?.takeIf { it.isNotEmpty() }
            ?.joinToString(prefix = "Related cards: "),
        insight["mechanics"]?.stringValues()?.takeIf { it.isNotEmpty() }
            ?.joinToString(prefix = "Mechanics: "),
        insight["archetypes"]?.stringValues()?.takeIf { it.isNotEmpty() }
            ?.joinToString(prefix = "Archetypes: "),
    ).joinToString("\n")

    private fun buildMetadata(article: Article, root: JsonNode, insight: JsonNode, index: Int): Map<String, Any> =
        buildMap {
            put("article_id", article.id!!)
            put("insight_index", index)
            put("title", article.title)
            put("slug", article.slug)
            put("url", article.url)
            put("favorite", article.favorite)
            article.publishedAt?.let { put("published_at", it.toString()) }
            put("article_type", root["article_type"]?.asString().orEmpty())
            put("processing_profile", root["processing_profile"]?.asString().orEmpty())
            put("insight_type", insight["type"]?.asString().orEmpty())
            put("subject", insight["subject"]?.asString().orEmpty())
            put("tags", insight["tags"]?.stringValues().orEmpty())
            put("keywords", article.keywords)
            put("related_cards", insight["related_cards"]?.stringValues().orEmpty())
            put("mechanics", insight["mechanics"]?.stringValues().orEmpty())
            put("archetypes", insight["archetypes"]?.stringValues().orEmpty())
            put("formats", insight["formats"]?.stringValues().orEmpty())
        }

    override fun destroy() {
        scope.cancel()
    }
}

private const val MAX_CONCURRENT_VECTOR_INDEXES = 3
private const val ARTICLE_SCHEMA_VERSION = 2

private fun buildDocumentId(articleId: Long, insightIndex: Int): String = UUID.nameUUIDFromBytes(
    "draftsim-article-$articleId-insight-$insightIndex".toByteArray(StandardCharsets.UTF_8),
).toString()

private fun JsonNode.stringValues(): List<String> =
    if (isArray) mapNotNull { it.asString()?.takeIf(String::isNotBlank) } else emptyList()
