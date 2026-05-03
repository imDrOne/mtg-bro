package xyz.candycrawler.draftsimparser.application.service

import org.springframework.stereotype.Component
import xyz.candycrawler.draftsimparser.domain.article.model.Article
import java.util.Locale

enum class ArticleType {
    CARD_REVIEW,
    MECHANIC_GUIDE,
    ARCHETYPE_GUIDE,
    DRAFT_GUIDE,
    FORMAT_STRATEGY,
    SET_OVERVIEW,
    GENERIC_MTG_KNOWLEDGE,
    IGNORE,
    ;

    companion object {
        fun from(value: String?): ArticleType =
            entries.firstOrNull { it.name == value.normalizeEnumValue() } ?: GENERIC_MTG_KNOWLEDGE
    }
}

enum class ProcessingProfile {
    CARD,
    MECHANIC,
    ARCHETYPE,
    DRAFT,
    FORMAT,
    SET,
    GENERIC,
    IGNORE,
    ;

    companion object {
        fun from(value: String?, articleType: ArticleType): ProcessingProfile =
            entries.firstOrNull { it.name == value.normalizeEnumValue() } ?: articleType.defaultProcessingProfile()
    }
}

data class ArticleAnalysisClassification(
    val articleType: ArticleType,
    val processingProfile: ProcessingProfile,
    val reason: String?,
    val confidence: Double?,
) {
    companion object {
        val DEFAULT = ArticleAnalysisClassification(
            articleType = ArticleType.GENERIC_MTG_KNOWLEDGE,
            processingProfile = ProcessingProfile.GENERIC,
            reason = "Classification unavailable",
            confidence = null,
        )
    }
}

@Component
class ArticleAnalysisPromptBuilder {

    fun buildClassificationPrompt(article: Article): String = """
        ARTICLE_CLASSIFICATION

        Classify this Draftsim MTG article before content analysis.
        Use only title, slug, URL, and extracted keywords. Do not invent facts from article content.
        Return ONLY valid JSON, no markdown and no surrounding text.

        Article title: ${article.title}
        Article slug: ${article.slug}
        Article URL: ${article.url}
        Extracted keywords: ${article.keywords.joinToString(", ")}

        Choose exactly one article_type:
        - card_review: article primarily evaluates individual cards or top-card lists.
        - mechanic_guide: article explains set mechanics, rules, or mechanic-specific payoffs/enablers.
        - archetype_guide: article explains a deck archetype, color pair, theme, or build-around shell.
        - draft_guide: article is mainly about Limited draft/sealed strategy, pick order, or format navigation.
        - format_strategy: article is mainly about constructed metagame, format advice, or gameplay strategy.
        - set_overview: article summarizes a release, set themes, products, or broad set-level changes.
        - generic_mtg_knowledge: useful MTG knowledge that does not fit the above categories.
        - ignore: ads, navigation, non-MTG content, or no useful deckbuilding/card-selection signal.

        Map article_type to processing_profile:
        - card_review -> card
        - mechanic_guide -> mechanic
        - archetype_guide -> archetype
        - draft_guide -> draft
        - format_strategy -> format
        - set_overview -> set
        - generic_mtg_knowledge -> generic
        - ignore -> ignore

        Return this JSON structure:
        {
          "article_type": "mechanic_guide",
          "processing_profile": "mechanic",
          "reason": "short reason for the classification",
          "confidence": 0.85
        }
    """.trimIndent()

    fun buildAnalysisPrompt(
        paragraph: String,
        article: Article,
        classification: ArticleAnalysisClassification,
    ): String = """
        ARTICLE_ANALYSIS

        Extract MTG knowledge from this article paragraph using the selected processing profile.
        Return ONLY valid JSON, no markdown and no surrounding text.
        Return null if the paragraph contains no useful MTG knowledge for deckbuilding, card selection, drafting, or format understanding.

        Article title: ${article.title}
        Article URL: ${article.url}
        Article slug: ${article.slug}
        Article type: ${classification.articleType.name.lowercase()}
        Processing profile: ${classification.processingProfile.name.lowercase()}
        Classification reason: ${classification.reason ?: "n/a"}
        Extracted article keywords: ${article.keywords.joinToString(", ")}

        Profile instructions:
        ${profileInstructions(classification.processingProfile)}

        Paragraph:
        $paragraph

        Return this JSON structure:
        {
          "type": "card | mechanic | archetype | format_strategy | draft_signal | set_context | generic",
          "subject": "card name, mechanic name, archetype name, set theme, or concise subject",
          "summary": "1-3 sentence expert summary",
          "deckbuilding_implications": ["concrete implications for choosing cards or building a deck"],
          "related_cards": ["exact card names mentioned or clearly relevant from the paragraph"],
          "mechanics": ["mechanic names mentioned or explained"],
          "archetypes": ["archetypes, color pairs, or themes"],
          "formats": ["Limited", "Standard", "Commander", "Pioneer", "Modern", etc.],
          "tags": ["short normalized tags"],
          "confidence": 0.85
        }
    """.trimIndent()

    private fun profileInstructions(profile: ProcessingProfile): String = when (profile) {
        ProcessingProfile.CARD -> """
                Focus on individual cards, role, power level, synergy, replacement candidates, and when the card belongs in a deck.
                Do not ignore non-card context if it changes how the card should be evaluated.
        """.trimIndent()

        ProcessingProfile.MECHANIC -> """
                Focus on how the mechanic works, what kinds of cards enable it, what payoffs it creates, and how it changes deck construction.
                Extract important cards only when they illustrate or materially support the mechanic.
        """.trimIndent()

        ProcessingProfile.ARCHETYPE -> """
                Focus on game plan, color pair/theme, enablers, payoffs, curve needs, removal/fixing requirements, and cards that signal the archetype is open.
        """.trimIndent()

        ProcessingProfile.DRAFT -> """
                Focus on Limited pick priorities, draft signals, format speed, synergy density, removal/fixing, sideboard notes, and cards whose value depends on draft context.
        """.trimIndent()

        ProcessingProfile.FORMAT -> """
                Focus on constructed format context, metagame pressure, matchup implications, card positioning, and strategic reasons to include or avoid cards.
        """.trimIndent()

        ProcessingProfile.SET -> """
                Focus on set-level themes, release mechanics, supported archetypes, product or card pool context, and broad implications for deckbuilding.
        """.trimIndent()

        ProcessingProfile.GENERIC -> """
                Extract any useful MTG insight. Prefer mechanics, archetypes, format context, and deckbuilding implications over forcing a card recommendation.
        """.trimIndent()

        ProcessingProfile.IGNORE -> """
                Return null unless the paragraph unexpectedly contains concrete MTG knowledge.
        """.trimIndent()
    }
}

private fun String?.normalizeEnumValue(): String? = this
    ?.trim()
    ?.replace('-', '_')
    ?.lowercase(Locale.US)
    ?.uppercase(Locale.US)

private fun ArticleType.defaultProcessingProfile(): ProcessingProfile = when (this) {
    ArticleType.CARD_REVIEW -> ProcessingProfile.CARD
    ArticleType.MECHANIC_GUIDE -> ProcessingProfile.MECHANIC
    ArticleType.ARCHETYPE_GUIDE -> ProcessingProfile.ARCHETYPE
    ArticleType.DRAFT_GUIDE -> ProcessingProfile.DRAFT
    ArticleType.FORMAT_STRATEGY -> ProcessingProfile.FORMAT
    ArticleType.SET_OVERVIEW -> ProcessingProfile.SET
    ArticleType.GENERIC_MTG_KNOWLEDGE -> ProcessingProfile.GENERIC
    ArticleType.IGNORE -> ProcessingProfile.IGNORE
}
