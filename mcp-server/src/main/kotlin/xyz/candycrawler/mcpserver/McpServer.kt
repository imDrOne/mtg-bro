package xyz.candycrawler.mcpserver

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO as ClientCIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import kotlinx.serialization.json.Json
import xyz.candycrawler.mcpserver.tools.analyzeTribalDepthSchema
import xyz.candycrawler.mcpserver.tools.getCollectionOverviewSchema
import xyz.candycrawler.mcpserver.tools.handleGetCollectionOverview
import xyz.candycrawler.mcpserver.tools.getCardSchema
import xyz.candycrawler.mcpserver.tools.handleAnalyzeTribalDepth
import xyz.candycrawler.mcpserver.tools.handleGetCard
import xyz.candycrawler.mcpserver.tools.handleListScryfallFormatCodes
import xyz.candycrawler.mcpserver.tools.handleSearchDraftsimArticles
import xyz.candycrawler.mcpserver.tools.handleSearchMyCards
import xyz.candycrawler.mcpserver.tools.handleSearchScryfall
import xyz.candycrawler.mcpserver.tools.listScryfallFormatCodesSchema
import xyz.candycrawler.mcpserver.tools.searchDraftsimArticlesSchema
import xyz.candycrawler.mcpserver.tools.searchMyCardsSchema
import xyz.candycrawler.mcpserver.tools.searchScryfallSchema
import xyz.candycrawler.mcpserver.tools.ToolContext

fun createServer(baseUrl: String, draftsimParserBaseUrl: String): Server {
    val httpClient = HttpClient(ClientCIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    val context = ToolContext(baseUrl = baseUrl, draftsimParserBaseUrl = draftsimParserBaseUrl, httpClient = httpClient)

    val server = Server(
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
    ) { request -> handleAnalyzeTribalDepth(context, request) }

    server.addTool(
        name = "get_collection_overview",
        description = "Get a high-level summary of your entire card collection: total unique cards, breakdown by color (W/U/B/R/G/C), type (creature/instant/etc), rarity, and top 10 tribes with their colors. Use this when the user asks what their collection looks like or wants an overview before planning a deck.",
        inputSchema = getCollectionOverviewSchema(),
    ) { request -> handleGetCollectionOverview(context, request) }

    server.addTool(
        name = "search_draftsim_articles",
        description = "Search Draftsim.com articles about MTG draft strategy, set reviews, and limited format guides. Returns article titles and full text content. Use this when the user asks about draft picks, limited strategy, card evaluations for draft, or wants to know what Draftsim says about a card or set.",
        inputSchema = searchDraftsimArticlesSchema(),
    ) { request -> handleSearchDraftsimArticles(context, request) }

    return server
}
