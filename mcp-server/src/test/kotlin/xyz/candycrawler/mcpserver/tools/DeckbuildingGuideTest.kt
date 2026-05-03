package xyz.candycrawler.mcpserver.tools

import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import xyz.candycrawler.mcpserver.FilteredMcpServer
import xyz.candycrawler.mcpserver.auth.ToolAccessConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DeckbuildingGuideTest {

    @Test
    fun `deckbuilding guide contains key MCP workflow tools`() {
        val guide = loadDeckbuildingGuide()

        assertTrue("search_my_cards" in guide)
        assertTrue("list_draftsim_articles" in guide)
        assertTrue("search_draftsim_articles" in guide)
        assertTrue("get_draftsim_articles" in guide)
        assertTrue("save_deck" in guide)
    }

    @Test
    fun `get deckbuilding guide tool returns markdown guide`() {
        val result = handleGetDeckbuildingGuide()
        val content = assertIs<TextContent>(result.content.single())

        assertTrue(content.text.startsWith("---"))
        assertTrue("MTG Bro Deckbuilding Workflow" in content.text)
    }

    @Test
    fun `deckbuilding guide resource is registered`() {
        val server = FilteredMcpServer(
            serverInfo = Implementation(name = "test", version = "0.0.1"),
            options = ServerOptions(
                capabilities = ServerCapabilities(
                    tools = ServerCapabilities.Tools(listChanged = true),
                    resources = ServerCapabilities.Resources(listChanged = true),
                )
            ),
            toolAccessConfig = ToolAccessConfig.loadFromResources(),
        )

        server.addDeckbuildingGuideResource()

        val resource = server.resources[DECKBUILDING_GUIDE_RESOURCE_URI]?.resource
        assertTrue(resource != null)
        assertEquals("mtg-bro-deckbuilding-skill", resource.name)
        assertEquals("text/markdown", resource.mimeType)
    }
}
