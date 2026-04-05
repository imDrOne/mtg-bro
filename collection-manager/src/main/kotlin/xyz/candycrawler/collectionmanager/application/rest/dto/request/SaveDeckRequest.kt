package xyz.candycrawler.collectionmanager.application.rest.dto.request

data class SaveDeckRequest(
    val name: String,
    val format: String,
    val comment: String? = null,
    val mainboard: List<DeckEntryRequest>,
    val sideboard: List<DeckEntryRequest> = emptyList(),
)

data class DeckEntryRequest(
    val setCode: String,
    val collectorNumber: String,
    val quantity: Int,
)
