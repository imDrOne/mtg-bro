package xyz.candycrawler.mcpserver

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO as ClientCIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import xyz.candycrawler.mcpserver.auth.ToolAccessConfig
import xyz.candycrawler.mcpserver.auth.currentUserRoles
import xyz.candycrawler.mcpserver.auth.isAuthEnabled
import kotlinx.serialization.json.Json
import xyz.candycrawler.mcpserver.tools.handleSaveDeck
import xyz.candycrawler.mcpserver.tools.saveDeckSchema
import xyz.candycrawler.mcpserver.tools.analyzeTribalDepthSchema
import xyz.candycrawler.mcpserver.tools.getCollectionOverviewSchema
import xyz.candycrawler.mcpserver.tools.handleGetCollectionOverview
import xyz.candycrawler.mcpserver.tools.getCardSchema
import xyz.candycrawler.mcpserver.tools.handleAnalyzeTribalDepth
import xyz.candycrawler.mcpserver.tools.handleGetCard
import xyz.candycrawler.mcpserver.tools.handleListScryfallFormatCodes
import xyz.candycrawler.mcpserver.tools.getDraftsimArticlesByIdSchema
import xyz.candycrawler.mcpserver.tools.handleGetDraftsimArticlesById
import xyz.candycrawler.mcpserver.tools.handleSearchDraftsimArticles
import xyz.candycrawler.mcpserver.tools.handleSearchMyCards
import xyz.candycrawler.mcpserver.tools.handleSearchScryfall
import xyz.candycrawler.mcpserver.tools.listScryfallFormatCodesSchema
import xyz.candycrawler.mcpserver.tools.searchDraftsimArticlesSchema
import xyz.candycrawler.mcpserver.tools.searchMyCardsSchema
import xyz.candycrawler.mcpserver.tools.searchScryfallSchema
import xyz.candycrawler.mcpserver.tools.ToolContext

fun createServer(baseUrl: String, draftsimParserBaseUrl: String): FilteredMcpServer {
    val httpClient = HttpClient(ClientCIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    val context = ToolContext(baseUrl = baseUrl, draftsimParserBaseUrl = draftsimParserBaseUrl, httpClient = httpClient)

    val server = FilteredMcpServer(
        serverInfo = Implementation(name = "mtg-bro", version = "1.0.0"),
        options = ServerOptions(
            capabilities = ServerCapabilities(tools = ServerCapabilities.Tools(listChanged = true)),
        ),
    )

    server.addTool(
        name = "search_my_cards",
        description = "Search cards in your local library (imported collection) with filters. PREFER this over get_my_collection — use colors, color_identity, type from user preferences to filter and avoid loading full collection. Returns card names, set, rarity, and how many copies you own.",
        inputSchema = searchMyCardsSchema(),
    ) { request -> handleSearchMyCards(context, request) }

    server.addTool(
        name = "search_scryfall",
        description = "Search Scryfall card database. Use Scryfall syntax: f:standard (format), c:bg (colors), t:creature (type), id:wu (identity). Example: 'f:standard c:bg' for Standard legal black-green cards.",
        inputSchema = searchScryfallSchema(),
    ) { request -> handleSearchScryfall(context, request) }

    server.addTool(
        name = "get_card",
        description = "Get a single card from your library by set code and collector number.",
        inputSchema = getCardSchema(),
    ) { request -> handleGetCard(context, request) }

    server.addTool(
        name = "list_scryfall_format_codes",
        description = "Returns format and color codes for search_scryfall and search_my_cards. Use filters to avoid loading large datasets.",
        inputSchema = listScryfallFormatCodesSchema(),
    ) { handleListScryfallFormatCodes() }

    server.addTool(
        name = "analyze_tribal_depth",
        description = "Analyze tribal depth for a given MTG creature type in your collection. Returns total card count, CMC distribution, role breakdown (creatures / kindred spells / tribal support cards), color spread, whether a lord or commander exists, and deck viability. Use this when the user asks about a specific tribe like Merfolk, Elf, Goblin, etc.",
        inputSchema = analyzeTribalDepthSchema(),
    ) { request ->
        checkAccess("analyze_tribal_depth")?.let { return@addTool it }
        handleAnalyzeTribalDepth(context, request)
    }

    server.addTool(
        name = "get_collection_overview",
        description = "Get a high-level summary of your entire card collection: total unique cards, breakdown by color (W/U/B/R/G/C), type (creature/instant/etc), rarity, and top 10 tribes with their colors. Use this when the user asks what their collection looks like or wants an overview before planning a deck.",
        inputSchema = getCollectionOverviewSchema(),
    ) { request ->
        checkAccess("get_collection_overview")?.let { return@addTool it }
        handleGetCollectionOverview(context, request)
    }

    server.addTool(
        name = "search_draftsim_articles",
        description = "Search favorited Draftsim.com articles about MTG draft strategy, set reviews, and limited format guides. Returns a lightweight list with id, title, slug and published date for browsing. Use get_draftsim_articles to fetch analyzed content for specific articles of interest.",
        inputSchema = searchDraftsimArticlesSchema(),
    ) { request ->
        checkAccess("search_draftsim_articles")?.let { return@addTool it }
        handleSearchDraftsimArticles(context, request)
    }

    server.addTool(
        name = "get_draftsim_articles",
        description = "Fetch analyzed MTG card knowledge from specific Draftsim articles by ID. Returns structured card evaluations (tiers, synergies, archetypes). Use after search_draftsim_articles to get content for articles of interest.",
        inputSchema = getDraftsimArticlesByIdSchema(),
    ) { request ->
        checkAccess("get_draftsim_articles")?.let { return@addTool it }
        handleGetDraftsimArticlesById(context, request)
    }

    server.addTool(
        name = "save_deck",
        description = """Save a finalized deck to your collection.
            IMPORTANT: Use search_my_cards first to find card IDs (the numeric 'id' field in results).
            Format rules: STANDARD requires mainboard >= 60 cards; SEALED and DRAFT require >= 40 cards.
            Max 4 copies of any single card.
            On success returns the saved deck ID.
            On validation failure (error response) the message explains what to fix — correct and retry.""",
        inputSchema = saveDeckSchema(),
    ) { request ->
        checkAccess("save_deck")?.let { return@addTool it }
        handleSaveDeck(context, request)
    }

    return server
}

internal suspend fun checkAccess(toolName: String): CallToolResult? {
    if (!isAuthEnabled()) return null
    val roles = currentUserRoles()
    if (ToolAccessConfig.hasAccess(toolName, roles)) return null
    return CallToolResult(
        content = listOf(TextContent("Access denied: tool '$toolName' requires PRO subscription")),
        isError = true,
    )
}
