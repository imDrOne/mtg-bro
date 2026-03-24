package xyz.candycrawler.collectionmanager.application.rest.dto.response

data class TribalStatsResponse(
    val tribe: String,
    val totalCards: Int,
    val byCmc: Map<String, Int>,
    val byRole: ByRoleResponse,
    val colorSpread: Map<String, Int>,
    val hasLord: Boolean,
    val hasCommander: Boolean,
    val deckViability: String,
) {
    data class ByRoleResponse(
        val creatures: Int,
        val tribalSpells: Int,
        val tribalSupport: Int,
    )
}
