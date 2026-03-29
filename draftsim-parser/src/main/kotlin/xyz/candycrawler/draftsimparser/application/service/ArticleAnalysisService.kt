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
import org.springframework.stereotype.Service
import xyz.candycrawler.draftsimparser.application.messaging.ArticleAnalysisConsumer
import xyz.candycrawler.draftsimparser.application.messaging.ArticleAnalysisMessage
import xyz.candycrawler.draftsimparser.application.port.LlmClient
import xyz.candycrawler.draftsimparser.domain.article.repository.ArticleRepository

private const val MAX_CONCURRENT_LLM_CALLS = 3

@Service
class ArticleAnalysisService(
    private val llmClient: LlmClient,
    private val articleRepository: ArticleRepository,
) : ArticleAnalysisConsumer {

    private val log = LoggerFactory.getLogger(javaClass)
    private val semaphore = Semaphore(MAX_CONCURRENT_LLM_CALLS)

    @EventListener
    fun handleArticleAnalysisMessage(message: ArticleAnalysisMessage) = runBlocking { consume(message) }

    override suspend fun consume(message: ArticleAnalysisMessage) {
        log.info("Analyzing article id={} slug={} ({} paragraphs)", message.articleId, message.slug, message.paragraphs.size)

        val results = coroutineScope {
            message.paragraphs.map { paragraph ->
                async(Dispatchers.IO) {
                    semaphore.withPermit {
                        runCatching {
                            llmClient.complete(buildPrompt(paragraph, message.slug, message.url))
                        }.getOrNull()
                    }
                }
            }.awaitAll()
        }

        val valid = results.filterNotNull().filter { it.isNotBlank() && it != "null" }
        if (valid.isEmpty()) {
            log.info("Article id={}: no valid LLM results, skipping analyzed_text update", message.articleId)
            return
        }

        val json = "[${valid.joinToString(",")}]"
        articleRepository.update(message.articleId) { it.copy(analyzedText = json) }
        log.info("Article id={}: saved {} card entries", message.articleId, valid.size)
    }

    private fun buildPrompt(paragraph: String, slug: String, url: String): String = """
        Extract knowledge from this MTG article paragraph.
        Return ONLY a valid JSON object for a single card, no other text.

        Article URL: $url
        Article slug: $slug
        Paragraph: $paragraph

        Return this structure:
        {
          "card": "exact card name",
          "set": "set code if determinable from context, else null",
          "chunk": "1-2 sentence expert summary focusing on why this card is good/bad and key synergies",
          "tags": ["array", "of", "relevant", "tags"],
          "combo_cards": ["cards that combo with this one, if mentioned"],
          "tier": "bomb/staple/roleplayer/bulk/null",
          "format_notes": "any format-specific observations or null"
        }

        Tags can include: combo, staple, bomb, removal, ramp, draw, aggro, control,
        midrange, reanimator, tribal, limited_bomb, budget, expensive, new_card, reprint

        If the paragraph does not mention a specific card, return: {"card": null}
    """.trimIndent()
}
