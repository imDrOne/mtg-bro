package xyz.candycrawler.draftsimparser.infrastructure.vector

import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.ai.document.Document
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.VectorStore
import kotlin.test.Test
import kotlin.test.assertEquals

class SpringAiArticleVectorStoreTest {

    private val vectorStore = mock<VectorStore>()
    private val articleVectorStore = SpringAiArticleVectorStore(vectorStore)

    @Test
    fun `search maps string article id metadata to long`() {
        whenever(vectorStore.similaritySearch(any<SearchRequest>())).thenReturn(
            listOf(
                Document.builder()
                    .id("4a7c12ff-2f23-4c8b-a37e-403a3582a5de")
                    .text("Repartee mechanic details")
                    .metadata(SpringAiArticleVectorStore.ARTICLE_ID, "1155")
                    .score(0.91)
                    .build()
            )
        )

        val result = articleVectorStore.search("repartee", topK = 8, similarityThreshold = 0.65)

        assertEquals(1, result.size)
        assertEquals(1155L, result.single().articleId)
        assertEquals("Repartee mechanic details", result.single().content)
    }
}
