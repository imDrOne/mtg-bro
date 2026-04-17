package xyz.candycrawler.mcpserver.auth

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ToolAccessConfigTest {

    @Test
    fun `FREE user can access free tools`() {
        val roles = listOf("FREE")
        assertTrue(ToolAccessConfig.hasAccess("search_my_cards", roles))
        assertTrue(ToolAccessConfig.hasAccess("search_scryfall", roles))
        assertTrue(ToolAccessConfig.hasAccess("get_card", roles))
        assertTrue(ToolAccessConfig.hasAccess("list_scryfall_format_codes", roles))
    }

    @Test
    fun `FREE user cannot access pro tools`() {
        val roles = listOf("FREE")
        assertFalse(ToolAccessConfig.hasAccess("analyze_tribal_depth", roles))
        assertFalse(ToolAccessConfig.hasAccess("get_collection_overview", roles))
        assertFalse(ToolAccessConfig.hasAccess("search_draftsim_articles", roles))
        assertFalse(ToolAccessConfig.hasAccess("get_draftsim_articles", roles))
        assertFalse(ToolAccessConfig.hasAccess("save_deck", roles))
    }

    @Test
    fun `PRO user can access all tools`() {
        val roles = listOf("PRO")
        assertTrue(ToolAccessConfig.hasAccess("search_my_cards", roles))
        assertTrue(ToolAccessConfig.hasAccess("analyze_tribal_depth", roles))
        assertTrue(ToolAccessConfig.hasAccess("save_deck", roles))
    }

    @Test
    fun `ADMIN user can access all tools`() {
        val roles = listOf("ADMIN")
        assertTrue(ToolAccessConfig.hasAccess("search_my_cards", roles))
        assertTrue(ToolAccessConfig.hasAccess("analyze_tribal_depth", roles))
        assertTrue(ToolAccessConfig.hasAccess("save_deck", roles))
    }

    @Test
    fun `user with no roles cannot access any tool`() {
        assertFalse(ToolAccessConfig.hasAccess("search_my_cards", emptyList()))
        assertFalse(ToolAccessConfig.hasAccess("analyze_tribal_depth", emptyList()))
    }

    @Test
    fun `user with both FREE and PRO roles can access all tools`() {
        val roles = listOf("FREE", "PRO")
        assertTrue(ToolAccessConfig.hasAccess("search_my_cards", roles))
        assertTrue(ToolAccessConfig.hasAccess("analyze_tribal_depth", roles))
    }
}
