package xyz.candycrawler.mcpserver

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpSendPipeline
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.serialization.json.Json
import xyz.candycrawler.mcpserver.auth.ToolAccessConfig
import xyz.candycrawler.mcpserver.auth.ToolAccessConfigData
import xyz.candycrawler.mcpserver.auth.currentUserRoles
import xyz.candycrawler.mcpserver.auth.currentUserToken
import xyz.candycrawler.mcpserver.auth.hasAccess
import xyz.candycrawler.mcpserver.auth.isAuthEnabled
import xyz.candycrawler.mcpserver.tools.DraftsimSearchConfig
import xyz.candycrawler.mcpserver.tools.ToolContext
import xyz.candycrawler.mcpserver.tools.addDeckbuildingGuideResource
import xyz.candycrawler.mcpserver.tools.analyzeTribalDepthSchema
import xyz.candycrawler.mcpserver.tools.getCardSchema
import xyz.candycrawler.mcpserver.tools.getCollectionOverviewSchema
import xyz.candycrawler.mcpserver.tools.getDeckbuildingGuideSchema
import xyz.candycrawler.mcpserver.tools.getDraftsimArticlesByIdSchema
import xyz.candycrawler.mcpserver.tools.handleAnalyzeTribalDepth
import xyz.candycrawler.mcpserver.tools.handleGetCard
import xyz.candycrawler.mcpserver.tools.handleGetCollectionOverview
import xyz.candycrawler.mcpserver.tools.handleGetDeckbuildingGuide
import xyz.candycrawler.mcpserver.tools.handleGetDraftsimArticlesById
import xyz.candycrawler.mcpserver.tools.handleListDraftsimArticles
import xyz.candycrawler.mcpserver.tools.handleListScryfallFormatCodes
import xyz.candycrawler.mcpserver.tools.handleSaveDeck
import xyz.candycrawler.mcpserver.tools.handleSearchDraftsimArticles
import xyz.candycrawler.mcpserver.tools.handleSearchMyCards
import xyz.candycrawler.mcpserver.tools.handleSearchScryfall
import xyz.candycrawler.mcpserver.tools.listDraftsimArticlesSchema
import xyz.candycrawler.mcpserver.tools.listScryfallFormatCodesSchema
import xyz.candycrawler.mcpserver.tools.saveDeckSchema
import xyz.candycrawler.mcpserver.tools.searchDraftsimArticlesSchema
import xyz.candycrawler.mcpserver.tools.searchMyCardsSchema
import xyz.candycrawler.mcpserver.tools.searchScryfallSchema
import io.ktor.client.engine.cio.CIO as ClientCIO

fun createServer(
    baseUrl: String,
    draftsimParserBaseUrl: String,
    draftsimSearchConfig: DraftsimSearchConfig = DraftsimSearchConfig(),
): FilteredMcpServer {
    val httpClient = HttpClient(ClientCIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }
    httpClient.sendPipeline.intercept(HttpSendPipeline.State) {
        val token = currentUserToken()
        if (token != null) {
            context.headers[HttpHeaders.Authorization] = "Bearer $token"
        }
    }

    val toolAccessConfig = ToolAccessConfig.loadFromResources()
    val context = ToolContext(
        baseUrl = baseUrl,
        draftsimParserBaseUrl = draftsimParserBaseUrl,
        httpClient = httpClient,
        draftsimSearchConfig = draftsimSearchConfig,
    )

    val server = FilteredMcpServer(
        serverInfo = Implementation(name = "mtg-bro", version = "1.0.0"),
        options = ServerOptions(
            capabilities = ServerCapabilities(
                tools = ServerCapabilities.Tools(listChanged = true),
                resources = ServerCapabilities.Resources(listChanged = true),
            ),
        ),
        toolAccessConfig = toolAccessConfig,
    )

    server.addDeckbuildingGuideResource()

    server.addTool(
        name = "search_my_cards",
        description = "Search cards in your local library (imported collection) with filters. " +
            "Prefer this over get_my_collection to avoid loading the full collection. " +
            "Returns card names, set, rarity, and owned copy counts.",
        inputSchema = searchMyCardsSchema(),
    ) { request -> handleSearchMyCards(context, request) }

    server.addTool(
        name = "search_scryfall",
        description = "Search Scryfall card database. Use Scryfall syntax: f:standard (format), " +
            "c:bg (colors), t:creature (type), id:wu (identity).",
        inputSchema = searchScryfallSchema(),
    ) { request -> handleSearchScryfall(context, request) }

    server.addTool(
        name = "get_card",
        description = "Get a single card from your library by set code and collector number.",
        inputSchema = getCardSchema(),
    ) { request -> handleGetCard(context, request) }

    server.addTool(
        name = "list_scryfall_format_codes",
        description = "Returns format and color codes for search_scryfall and search_my_cards. " +
            "Use filters to avoid loading large datasets.",
        inputSchema = listScryfallFormatCodesSchema(),
    ) { handleListScryfallFormatCodes() }

    server.addTool(
        name = "get_deckbuilding_guide",
        description = "Returns the mtg-bro deckbuilding workflow guide for AI agents using this MCP server. " +
            "Read this before building, tuning, or saving a Magic deck.",
        inputSchema = getDeckbuildingGuideSchema(),
    ) { request -> handleGetDeckbuildingGuide(request) }

    server.addTool(
        name = "analyze_tribal_depth",
        description = "Analyze tribal depth for a given MTG creature type in your collection. " +
            "Returns counts, CMC distribution, roles, colors, lord/commander presence, and deck viability.",
        inputSchema = analyzeTribalDepthSchema(),
    ) { request ->
        checkAccess("analyze_tribal_depth", toolAccessConfig)?.let { return@addTool it }
        handleAnalyzeTribalDepth(context, request)
    }

    server.addTool(
        name = "get_collection_overview",
        description = "Get a high-level summary of your entire card collection: unique cards, colors, " +
            "types, rarity, and top tribes with colors.",
        inputSchema = getCollectionOverviewSchema(),
    ) { request ->
        checkAccess("get_collection_overview", toolAccessConfig)?.let { return@addTool it }
        handleGetCollectionOverview(context, request)
    }

    server.addTool(
        name = "search_draftsim_articles",
        description = "Semantically search favorited Draftsim.com articles about MTG draft strategy, " +
            "set reviews, mechanics, archetypes, and limited format guides.",
        inputSchema = searchDraftsimArticlesSchema(),
    ) { request ->
        checkAccess("search_draftsim_articles", toolAccessConfig)?.let { return@addTool it }
        handleSearchDraftsimArticles(context, request)
    }

    server.addTool(
        name = "list_draftsim_articles",
        description = "List compact Draftsim article metadata by optional keyword query. " +
            "Use this to discover article IDs before get_draftsim_articles.",
        inputSchema = listDraftsimArticlesSchema(),
    ) { request ->
        checkAccess("list_draftsim_articles", toolAccessConfig)?.let { return@addTool it }
        handleListDraftsimArticles(context, request)
    }

    server.addTool(
        name = "get_draftsim_articles",
        description = "Fetch analyzed MTG card knowledge from specific Draftsim articles by ID. " +
            "Returns structured card evaluations such as tiers, synergies, and archetypes.",
        inputSchema = getDraftsimArticlesByIdSchema(),
    ) { request ->
        checkAccess("get_draftsim_articles", toolAccessConfig)?.let { return@addTool it }
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
        checkAccess("save_deck", toolAccessConfig)?.let { return@addTool it }
        handleSaveDeck(context, request)
    }

    return server
}

internal suspend fun checkAccess(toolName: String, config: ToolAccessConfigData): CallToolResult? {
    if (!isAuthEnabled()) return null
    val roles = currentUserRoles()
    if (config.hasAccess(toolName, roles)) return null
    return CallToolResult(
        content = listOf(TextContent("Access denied: tool '$toolName' requires PRO subscription")),
        isError = true,
    )
}
