package xyz.candycrawler.draftsimparser.application.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import xyz.candycrawler.draftsimparser.application.messaging.ArticleAnalysisConsumer
import xyz.candycrawler.draftsimparser.application.messaging.ArticleAnalysisMessage
import xyz.candycrawler.draftsimparser.application.port.LlmClient
import xyz.candycrawler.draftsimparser.domain.article.model.Article
import xyz.candycrawler.draftsimparser.domain.article.repository.ArticleRepository
import xyz.candycrawler.draftsimparser.domain.article.repository.QueryArticleRepository
import java.time.LocalDateTime

private const val MAX_CONCURRENT_LLM_CALLS = 3

@Service
class ArticleAnalysisService(
    private val llmClient: LlmClient,
    private val articleRepository: ArticleRepository,
    private val queryArticleRepository: QueryArticleRepository,
    private val promptBuilder: ArticleAnalysisPromptBuilder,
    private val objectMapper: ObjectMapper,
    private val vectorIndexService: ArticleVectorIndexService,
) : ArticleAnalysisConsumer {

    private val log = LoggerFactory.getLogger(javaClass)
    private val semaphore = Semaphore(MAX_CONCURRENT_LLM_CALLS)

    @Async
    @EventListener
    fun handleArticleAnalysisMessage(message: ArticleAnalysisMessage) = runBlocking { consume(message) }

    override suspend fun consume(message: ArticleAnalysisMessage) {
        val article = queryArticleRepository.findById(message.articleId)
        val paragraphs = article.textContent
            ?.split("\n\n")
            ?.filter { it.isNotBlank() }
            ?: emptyList()

        if (paragraphs.isEmpty()) {
            log.info("Article id={}: no paragraphs, skipping analysis", message.articleId)
            return
        }

        log.info("Article id={} slug={}: starting analysis ({} paragraphs)", article.id, article.slug, paragraphs.size)
        articleRepository.update(message.articleId) { it.copy(analyzStartedAt = LocalDateTime.now()) }

        runCatching {
            val classification = classify(article)
            if (classification.processingProfile == ProcessingProfile.IGNORE) {
                val updated = articleRepository.update(message.articleId) {
                    it.copy(analyzedText = buildAnalysisJson(article, classification, emptyList()), analyzEndedAt = LocalDateTime.now())
                }
                vectorIndexService.replaceIndex(updated)
                log.info("Article id={}: classified as ignore, skipping paragraph analysis", message.articleId)
                return@runCatching
            }

            val results = coroutineScope {
                paragraphs.map { paragraph ->
                    async(Dispatchers.IO) {
                        semaphore.withPermit {
                            runCatching {
                                llmClient.complete(promptBuilder.buildAnalysisPrompt(paragraph, article, classification))
                            }.getOrNull()
                        }
                    }
                }.awaitAll()
            }

            val insights = results.mapNotNull { parseInsight(it) }
            val json = buildAnalysisJson(article, classification, insights)

            val updated = articleRepository.update(message.articleId) {
                it.copy(analyzedText = json, analyzEndedAt = LocalDateTime.now())
            }
            vectorIndexService.replaceIndex(updated)
            log.info("Article id={}: analysis done, {} insight entries saved", message.articleId, insights.size)
        }.onFailure { ex ->
            log.error("Article id={}: analysis failed", message.articleId, ex)
            articleRepository.update(message.articleId) {
                it.copy(errorMsg = ex.message?.take(1000), analyzEndedAt = LocalDateTime.now())
            }
        }
    }

    private suspend fun classify(article: Article): ArticleAnalysisClassification {
        val response = llmClient.complete(promptBuilder.buildClassificationPrompt(article))
        val json = response.cleanJsonResponse() ?: return ArticleAnalysisClassification.DEFAULT
        return runCatching {
            val node = objectMapper.readTree(json)
            val articleType = ArticleType.from(node["article_type"]?.asString())
            ArticleAnalysisClassification(
                articleType = articleType,
                processingProfile = ProcessingProfile.from(node["processing_profile"]?.asString(), articleType),
                reason = node["reason"]?.asString(),
                confidence = node["confidence"]?.takeIf { it.isNumber }?.asDouble(),
            )
        }.getOrElse {
            log.warn("Article id={}: failed to parse classification response", article.id, it)
            ArticleAnalysisClassification.DEFAULT
        }
    }

    private fun parseInsight(response: String?): JsonNode? {
        val json = response.cleanJsonResponse() ?: return null
        return runCatching {
            objectMapper.readTree(json).takeUnless { it.isNull }
        }.getOrNull()
    }

    private fun buildAnalysisJson(
        article: Article,
        classification: ArticleAnalysisClassification,
        insights: List<JsonNode>,
    ): String =
        objectMapper.writeValueAsString(
            mapOf(
                "schema_version" to 2,
                "article_type" to classification.articleType.name.lowercase(),
                "processing_profile" to classification.processingProfile.name.lowercase(),
                "classification" to mapOf(
                    "reason" to classification.reason,
                    "confidence" to classification.confidence,
                ),
                "keywords" to article.keywords,
                "insights" to insights,
            )
        )

    private fun String?.cleanJsonResponse(): String? {
        val trimmed = this?.trim().orEmpty()
        if (trimmed.isBlank() || trimmed == "null") return null
        return trimmed
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
            .takeIf { it.isNotBlank() && it != "null" }
    }
}
