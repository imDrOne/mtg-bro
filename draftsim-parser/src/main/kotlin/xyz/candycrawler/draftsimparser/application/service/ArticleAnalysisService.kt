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
import xyz.candycrawler.draftsimparser.application.messaging.ArticleAnalysisConsumer
import xyz.candycrawler.draftsimparser.application.messaging.ArticleAnalysisMessage
import xyz.candycrawler.draftsimparser.application.port.LlmClient
import xyz.candycrawler.draftsimparser.domain.article.repository.ArticleRepository
import xyz.candycrawler.draftsimparser.domain.article.repository.QueryArticleRepository
import java.time.LocalDateTime

private const val MAX_CONCURRENT_LLM_CALLS = 3

@Service
class ArticleAnalysisService(
    private val llmClient: LlmClient,
    private val articleRepository: ArticleRepository,
    private val queryArticleRepository: QueryArticleRepository,
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
            val results = coroutineScope {
                paragraphs.map { paragraph ->
                    async(Dispatchers.IO) {
                        semaphore.withPermit {
                            runCatching {
                                llmClient.complete(buildPrompt(paragraph, article.slug, article.url))
                            }.getOrNull()
                        }
                    }
                }.awaitAll()
            }

            val valid = results.filterNotNull().filter { it.isNotBlank() && it != "null" }
            val json = if (valid.isNotEmpty()) "[${valid.joinToString(",")}]" else null

            articleRepository.update(message.articleId) {
                it.copy(analyzedText = json, analyzEndedAt = LocalDateTime.now())
            }
            log.info("Article id={}: analysis done, {} card entries saved", message.articleId, valid.size)
        }.onFailure { ex ->
            log.error("Article id={}: analysis failed", message.articleId, ex)
            articleRepository.update(message.articleId) {
                it.copy(errorMsg = ex.message?.take(1000), analyzEndedAt = LocalDateTime.now())
            }
        }
    }

    private fun buildPrompt(paragraph: String, slug: String, url: String): String = """
        Extract MTG knowledge from this article paragraph. The paragraph may be about a specific card, an archetype, a set mechanic, or a draft strategy.
        Return ONLY valid JSON, no other text. Return null (not an object) if the paragraph contains no useful MTG knowledge.

        Article URL: $url
        Article slug: $slug
        Paragraph: $paragraph

        Return this JSON structure:
        {
          "card": "exact card name, or null if paragraph is not about a specific card",
          "archetype": "archetype or mechanic name (e.g. 'UW Control', 'Convoke', 'Domain Ramp', 'Aggro') or null if not applicable",
          "set": "set code if determinable (e.g. FDN, BLB, DSK), else null",
          "chunk": "1-3 sentence expert summary: why this card/archetype is good/bad, key synergies, or how the mechanic works in context",
          "tags": ["array", "of", "relevant", "tags"],
          "combo_cards": ["cards that combo with this one, if mentioned, else empty array"],
          "tier": "for cards: bomb/staple/roleplayer/bulk; for archetypes: S/A/B/C/D; null if unclear",
          "format_notes": "format-specific observations (Limited, Standard, Pioneer, etc.) or null"
        }

        Tags can include:
        - Card role: combo, staple, bomb, removal, ramp, draw, counter, tutor, finisher, enabler
        - Strategy: aggro, control, midrange, reanimator, tempo, prison, storm
        - Tribe/theme: tribal, vampire, zombie, goblin, elf, artifact, enchantment, graveyard
        - Draft: limited_bomb, limited_bulk, late_pick, early_pick, archetype_payoff, filler
        - Meta: budget, expensive, new_card, reprint, sleeper, overrated, underrated
        - Mechanic: convoke, discover, surveil, ward, populate, proliferate (use actual mechanic name)
        - Archetype tag: archetype (always include when "archetype" field is non-null)

        Return null if the paragraph is introductory text, navigation, ads, or contains no actionable MTG knowledge.
    """.trimIndent()
}
