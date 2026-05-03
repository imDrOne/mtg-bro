package xyz.candycrawler.collectionmanager.application.rest.dto.response

data class CollectionOverviewResponse(
    val totalCards: Int,
    val byColor: Map<String, Int>,
    val byType: Map<String, Int>,
    val topTribes: List<TopTribeResponse>,
    val byRarity: Map<String, Int>,
) {
    data class TopTribeResponse(val name: String, val count: Int, val colors: String)
}
