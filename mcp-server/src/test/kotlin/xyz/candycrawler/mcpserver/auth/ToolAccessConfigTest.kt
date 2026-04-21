package xyz.candycrawler.mcpserver.auth

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ToolAccessConfigTest {

    private val config: ToolAccessConfigData = ToolAccessConfig.loadFromResources()

    @Test
    fun `FREE user can access free tools`() {
        val roles = listOf("FREE")
        assertTrue(config.hasAccess("search_my_cards", roles))
        assertTrue(config.hasAccess("search_scryfall", roles))
        assertTrue(config.hasAccess("get_card", roles))
        assertTrue(config.hasAccess("list_scryfall_format_codes", roles))
    }

    @Test
    fun `FREE user cannot access pro tools`() {
        val roles = listOf("FREE")
        assertFalse(config.hasAccess("analyze_tribal_depth", roles))
        assertFalse(config.hasAccess("get_collection_overview", roles))
        assertFalse(config.hasAccess("search_draftsim_articles", roles))
        assertFalse(config.hasAccess("get_draftsim_articles", roles))
        assertFalse(config.hasAccess("save_deck", roles))
    }

    @Test
    fun `PRO user can access all tools`() {
        val roles = listOf("PRO")
        assertTrue(config.hasAccess("search_my_cards", roles))
        assertTrue(config.hasAccess("analyze_tribal_depth", roles))
        assertTrue(config.hasAccess("save_deck", roles))
    }

    @Test
    fun `ADMIN user can access all tools via wildcard`() {
        val roles = listOf("ADMIN")
        assertTrue(config.hasAccess("search_my_cards", roles))
        assertTrue(config.hasAccess("analyze_tribal_depth", roles))
        assertTrue(config.hasAccess("save_deck", roles))
        // Wildcard also grants access to hypothetical future tools
        assertTrue(config.hasAccess("some_future_tool", roles))
    }

    @Test
    fun `user with no roles cannot access any tool`() {
        assertFalse(config.hasAccess("search_my_cards", emptyList()))
        assertFalse(config.hasAccess("analyze_tribal_depth", emptyList()))
    }

    @Test
    fun `user with both FREE and PRO roles can access all tools`() {
        val roles = listOf("FREE", "PRO")
        assertTrue(config.hasAccess("search_my_cards", roles))
        assertTrue(config.hasAccess("analyze_tribal_depth", roles))
    }

    @Test
    fun `config loaded from YAML stream matches resource loading`() {
        val yamlContent = """
            roles:
              FREE:
                - search_my_cards
              ADMIN: "*"
            default_allowed_tools: []
        """.trimIndent()
        val streamConfig = ToolAccessConfig.load(yamlContent.byteInputStream())
        assertTrue(streamConfig.hasAccess("search_my_cards", listOf("FREE")))
        assertFalse(streamConfig.hasAccess("some_other_tool", listOf("FREE")))
        assertTrue(streamConfig.hasAccess("anything", listOf("ADMIN")))
    }

    @Test
    fun `tool not in config denies access for FREE and PRO`() {
        val roles = listOf("FREE", "PRO")
        // A tool that does not exist in the config should not be accessible
        assertFalse(config.hasAccess("nonexistent_tool", roles))
    }
}
