package xyz.candycrawler.mcpserver.tools

import kotlin.test.Test
import kotlin.test.assertEquals

class DraftsimSearchConfigTest {

    @Test
    fun `parseDraftsimSimilarityThresholds keeps valid csv order`() {
        val thresholds = parseDraftsimSimilarityThresholds("0.9, 0.7,0.5", DEFAULT_DRAFTSIM_SIMILARITY_THRESHOLDS)

        assertEquals(listOf(0.9, 0.7, 0.5), thresholds)
    }

    @Test
    fun `parseDraftsimSimilarityThresholds drops duplicates and invalid values`() {
        val thresholds =
            parseDraftsimSimilarityThresholds("0.9, nope, 1.5, 0.7, 0.9, -0.1", DEFAULT_DRAFTSIM_SIMILARITY_THRESHOLDS)

        assertEquals(listOf(0.9, 0.7), thresholds)
    }

    @Test
    fun `parseDraftsimSimilarityThresholds uses defaults for blank values`() {
        val thresholds = parseDraftsimSimilarityThresholds("  ", DEFAULT_DRAFTSIM_SIMILARITY_THRESHOLDS)

        assertEquals(DEFAULT_DRAFTSIM_SIMILARITY_THRESHOLDS, thresholds)
    }

    @Test
    fun `parseDraftsimSimilarityThresholds uses defaults when all values are invalid`() {
        val thresholds = parseDraftsimSimilarityThresholds("nope, 1.5, -0.1", DEFAULT_DRAFTSIM_SIMILARITY_THRESHOLDS)

        assertEquals(DEFAULT_DRAFTSIM_SIMILARITY_THRESHOLDS, thresholds)
    }

    @Test
    fun `draftsimSearchConfigFromEnv reads thresholds from csv env`() {
        val config = draftsimSearchConfigFromEnv { name ->
            if (name == DRAFTSIM_SIMILARITY_THRESHOLDS_ENV) "0.95,0.75,0.55" else null
        }

        assertEquals(listOf(0.95, 0.75, 0.55), config.semanticSimilarityThresholds)
    }

    @Test
    fun `draftsimSearchConfigFromEnv accepts explicit default thresholds`() {
        val config = draftsimSearchConfigFromEnv { name ->
            if (name == DRAFTSIM_SIMILARITY_THRESHOLDS_ENV) "0.80,0.65,0.50" else null
        }

        assertEquals(DEFAULT_DRAFTSIM_SIMILARITY_THRESHOLDS, config.semanticSimilarityThresholds)
    }
}
