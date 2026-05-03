package xyz.candycrawler.mcpserver

import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import xyz.candycrawler.mcpserver.auth.ToolAccessConfig
import xyz.candycrawler.mcpserver.auth.UserRolesElement
import kotlin.test.Test
import kotlin.test.assertEquals

class FilteredMcpServerTest {

    private val config = ToolAccessConfig.loadFromResources()

    private fun makeServer(): FilteredMcpServer {
        val server = FilteredMcpServer(
            serverInfo = Implementation(name = "test", version = "0.0.1"),
            options = ServerOptions(
                capabilities = ServerCapabilities(
                    tools = ServerCapabilities.Tools(listChanged = true),
                    resources = ServerCapabilities.Resources(listChanged = true),
                ),
            ),
            toolAccessConfig = config,
        )
        listOf(
            "search_my_cards", "search_scryfall", "get_card", "list_scryfall_format_codes",
            "analyze_tribal_depth", "get_collection_overview",
            "get_deckbuilding_guide", "list_draftsim_articles",
            "search_draftsim_articles", "get_draftsim_articles", "save_deck",
        ).forEach { name ->
            server.addTool(name = name, description = name) { _ -> error("stub") }
        }
        return server
    }

    @Test
    fun `FREE user sees only free tools`() = runBlocking {
        val server = makeServer()
        val visible = withContext(UserRolesElement(listOf("FREE"))) {
            server.visibleToolNames()
        }
        assertEquals(
            setOf(
                "search_my_cards",
                "search_scryfall",
                "get_card",
                "list_scryfall_format_codes",
                "get_deckbuilding_guide",
            ),
            visible.toSet(),
        )
    }

    @Test
    fun `PRO user sees all tools`() = runBlocking {
        val server = makeServer()
        val visible = withContext(UserRolesElement(listOf("PRO"))) {
            server.visibleToolNames()
        }
        assertEquals(11, visible.size)
    }

    @Test
    fun `ADMIN user sees all tools`() = runBlocking {
        val server = makeServer()
        val visible = withContext(UserRolesElement(listOf("ADMIN"))) {
            server.visibleToolNames()
        }
        assertEquals(11, visible.size)
    }

    @Test
    fun `no auth context returns all tools (stdio mode)`() = runBlocking {
        val server = makeServer()
        val visible = server.visibleToolNames()
        assertEquals(11, visible.size)
    }
}
