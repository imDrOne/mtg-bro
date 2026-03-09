package xyz.candycrawler.collectionmanager.application.service

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import xyz.candycrawler.collectionmanager.application.parser.TcgPlayerFileParser
import xyz.candycrawler.collectionmanager.infrastructure.client.scryfall.ScryfallApiClient
import xyz.candycrawler.collectionmanager.infrastructure.client.scryfall.dto.request.CardIdentifier
import xyz.candycrawler.collectionmanager.infrastructure.client.scryfall.dto.request.ScryfallCollectionRequest
import xyz.candycrawler.collectionmanager.infrastructure.client.scryfall.dto.response.ScryfallCardResponse
import xyz.candycrawler.collectionmanager.infrastructure.client.scryfall.dto.response.ScryfallCollectionResponse
import xyz.candycrawler.collectionmanager.infrastructure.client.scryfall.mapper.ScryfallCardResponseToCardMapper
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TcgPlayerImportServiceTest {

    private val parser: TcgPlayerFileParser = TcgPlayerFileParser()
    private val scryfallApiClient: ScryfallApiClient = mock()
    private val scryfallMapper: ScryfallCardResponseToCardMapper = ScryfallCardResponseToCardMapper()
    private val persistenceService: CollectionPersistenceService = mock()

    private val service = TcgPlayerImportService(
        parser, scryfallApiClient, scryfallMapper, persistenceService,
    )

    @Test
    fun `import parses file, fetches from scryfall and persists`() = runTest {
        val fileContent = """
            2 Eclipsed Elf [ECL] 218
            3 Lys Alana Dignitary [ECL] 180
        """.trimIndent()

        val scryfallResponse = buildScryfallResponse(
            data = listOf(
                buildScryfallCardResponse(setCode = "ecl", collectorNumber = "218"),
                buildScryfallCardResponse(setCode = "ecl", collectorNumber = "180"),
            ),
            notFound = emptyList(),
        )
        whenever(scryfallApiClient.fetchCollection(any())).thenReturn(scryfallResponse)
        whenever(persistenceService.saveImportedData(any(), any())).thenReturn(2)

        val result = service.import(fileContent)

        assertEquals(2, result.importedCount)
        assertTrue(result.notFound.isEmpty())
    }

    @Test
    fun `import merges duplicate entries before calling scryfall`() = runTest {
        val fileContent = """
            2 Eclipsed Elf [ECL] 218
            3 Eclipsed Elf [ECL] 218
        """.trimIndent()

        val scryfallResponse = buildScryfallResponse(
            data = listOf(buildScryfallCardResponse(setCode = "ecl", collectorNumber = "218")),
            notFound = emptyList(),
        )
        whenever(scryfallApiClient.fetchCollection(any())).thenReturn(scryfallResponse)
        whenever(persistenceService.saveImportedData(any(), any())).thenReturn(1)

        service.import(fileContent)

        val requestCaptor = argumentCaptor<ScryfallCollectionRequest>()
        verify(scryfallApiClient).fetchCollection(requestCaptor.capture())

        val identifiers = requestCaptor.firstValue.identifiers
        assertEquals(1, identifiers.size)
        assertEquals(CardIdentifier(set = "ecl", collectorNumber = "218"), identifiers.single())
    }

    @Test
    fun `import passes merged quantities to persistenceService`() = runTest {
        val fileContent = """
            2 Eclipsed Elf [ECL] 218
            3 Eclipsed Elf [ECL] 218
        """.trimIndent()

        val scryfallResponse = buildScryfallResponse(
            data = listOf(buildScryfallCardResponse(setCode = "ecl", collectorNumber = "218")),
            notFound = emptyList(),
        )
        whenever(scryfallApiClient.fetchCollection(any())).thenReturn(scryfallResponse)
        whenever(persistenceService.saveImportedData(any(), any())).thenReturn(1)

        service.import(fileContent)

        val quantityCaptor = argumentCaptor<Map<Pair<String, String>, Int>>()
        verify(persistenceService).saveImportedData(any(), quantityCaptor.capture())

        val quantityByKey = quantityCaptor.firstValue
        assertEquals(5, quantityByKey["ecl" to "218"])
    }

    @Test
    fun `import returns notFound from scryfall response`() = runTest {
        val fileContent = "1 Unknown Card [XXX] 999"

        val notFound = listOf(CardIdentifier(set = "xxx", collectorNumber = "999"))
        val scryfallResponse = buildScryfallResponse(data = emptyList(), notFound = notFound)
        whenever(scryfallApiClient.fetchCollection(any())).thenReturn(scryfallResponse)
        whenever(persistenceService.saveImportedData(any(), any())).thenReturn(0)

        val result = service.import(fileContent)

        assertEquals(1, result.notFound.size)
        assertEquals(notFound.single(), result.notFound.single())
    }

    @Test
    fun `import calls scryfall in batches when more than 75 unique cards`() = runTest {
        val fileContent = (1..80).joinToString("\n") { n ->
            "1 Card $n [DSK] ${n.toString().padStart(3, '0')}"
        }

        whenever(scryfallApiClient.fetchCollection(any())).thenReturn(
            buildScryfallResponse(data = emptyList(), notFound = emptyList())
        )
        whenever(persistenceService.saveImportedData(any(), any())).thenReturn(0)

        service.import(fileContent)

        // 80 unique cards → 2 batches (75 + 5)
        verify(scryfallApiClient, org.mockito.kotlin.times(2)).fetchCollection(any())
    }

    @Test
    fun `import with empty file returns zero imported and no notFound`() = runTest {
        whenever(persistenceService.saveImportedData(any(), any())).thenReturn(0)

        val result = service.import("")

        assertEquals(0, result.importedCount)
        assertTrue(result.notFound.isEmpty())
    }

    private fun buildScryfallResponse(
        data: List<ScryfallCardResponse>,
        notFound: List<CardIdentifier>,
    ) = ScryfallCollectionResponse(data = data, notFound = notFound)

    private fun buildScryfallCardResponse(
        setCode: String,
        collectorNumber: String,
    ) = ScryfallCardResponse(
        id = UUID.randomUUID(),
        oracleId = UUID.randomUUID(),
        name = "Test Card",
        lang = "en",
        layout = "normal",
        manaCost = "{G}",
        cmc = 1.0,
        typeLine = "Creature — Elf",
        oracleText = null,
        colors = listOf("G"),
        colorIdentity = listOf("G"),
        keywords = emptyList(),
        power = "1",
        toughness = "1",
        loyalty = null,
        setCode = setCode,
        setName = "Test Set",
        collectorNumber = collectorNumber,
        rarity = "common",
        releasedAt = "2024-09-27",
        imageUris = null,
        prices = null,
        flavorText = null,
        artist = null,
    )
}
