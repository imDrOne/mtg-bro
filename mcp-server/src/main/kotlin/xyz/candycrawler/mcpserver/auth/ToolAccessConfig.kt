package xyz.candycrawler.mcpserver.auth

object ToolAccessConfig {

    private val FREE_TOOLS = setOf(
        "search_my_cards",
        "search_scryfall",
        "get_card",
        "list_scryfall_format_codes",
    )

    private val PRO_TOOLS = FREE_TOOLS + setOf(
        "analyze_tribal_depth",
        "get_collection_overview",
        "search_draftsim_articles",
        "get_draftsim_articles",
        "save_deck",
    )

    private val ACCESS: Map<String, Set<String>> = mapOf(
        "FREE" to FREE_TOOLS,
        "PRO" to PRO_TOOLS,
        "ADMIN" to PRO_TOOLS,
    )

    fun hasAccess(toolName: String, roles: List<String>): Boolean =
        roles.any { role -> ACCESS[role]?.contains(toolName) == true }
}
