package xyz.candycrawler.draftsimparser.application.service

import org.springframework.stereotype.Component
import kotlin.math.min

@Component
class ArticleKeywordExtractor {

    fun extract(text: String?, limit: Int = DEFAULT_LIMIT): List<String> {
        if (text.isNullOrBlank() || limit <= 0) return emptyList()

        val tokens = TOKEN_REGEX.findAll(removeWordPressReplacementMarker(text).lowercase())
            .map { it.value.trim('\'') }
            .filter { it.length >= MIN_TOKEN_LENGTH && it !in STOP_WORDS }
            .toList()

        if (tokens.isEmpty()) return emptyList()

        val scores = linkedMapOf<String, Double>()
        tokens.groupingBy { it }.eachCount().forEach { (token, count) ->
            scores[token] = score(count, token.length, isPhrase = false)
        }
        tokens.windowed(size = 2).forEach { pair ->
            if (pair[0] != pair[1]) {
                val phrase = "${pair[0]} ${pair[1]}"
                scores[phrase] = (scores[phrase] ?: 0.0) + score(1, phrase.length, isPhrase = true)
            }
        }

        return scores.entries
            .sortedWith(
                compareByDescending<Map.Entry<String, Double>> { it.value }
                    .thenBy { it.key }
            )
            .take(min(limit, scores.size))
            .map { it.key }
    }

    private fun score(count: Int, length: Int, isPhrase: Boolean): Double {
        val phraseBoost = if (isPhrase) 1.4 else 1.0
        val lengthBoost = 1.0 + min(length, 20) / 100.0
        return count * phraseBoost * lengthBoost
    }

    private fun removeWordPressReplacementMarker(text: String): String =
        WORDPRESS_REPLACED_REGEX.replace(text, " ")

    companion object {
        const val DEFAULT_LIMIT = 20
        private const val MIN_TOKEN_LENGTH = 3
        private val TOKEN_REGEX = Regex("[a-z][a-z0-9']+")
        private val WORDPRESS_REPLACED_REGEX = Regex("\\bREPLACED\\b", RegexOption.IGNORE_CASE)
        private val STOP_WORDS = setOf(
            "about", "above", "after", "again", "against", "also", "although", "always", "among",
            "and", "another", "any", "are", "around", "because", "been", "before", "being", "below",
            "between", "both", "but", "can", "cannot", "could", "did", "does", "doing", "down",
            "during", "each", "either", "else", "even", "ever", "every", "few", "for", "from",
            "had", "has", "have", "having", "here", "how", "into", "its", "just", "like", "may",
            "might", "more", "most", "much", "must", "not", "off", "often", "once", "only", "other",
            "our", "out", "over", "own", "same", "she", "should", "since", "some", "such", "than",
            "that", "the", "their", "them", "then", "there", "these", "they", "this", "those", "through",
            "too", "under", "until", "very", "was", "way", "were", "what", "when", "where", "whether",
            "which", "while", "who", "why", "will", "with", "within", "without", "would", "you", "your",
            "replaced"
        )
    }
}
