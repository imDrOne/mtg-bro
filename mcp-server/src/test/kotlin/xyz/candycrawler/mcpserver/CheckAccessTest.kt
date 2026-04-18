package xyz.candycrawler.mcpserver

import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import xyz.candycrawler.mcpserver.auth.UserRolesElement
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CheckAccessTest {

    @Test
    fun `returns null when auth is not enabled`() = runBlocking {
        val result = checkAccess("analyze_tribal_depth")
        assertNull(result)
    }

    @Test
    fun `returns null when user has required PRO role`() = runBlocking {
        withContext(UserRolesElement(listOf("PRO"))) {
            val result = checkAccess("analyze_tribal_depth")
            assertNull(result)
        }
    }

    @Test
    fun `returns null for FREE tool regardless of role`() = runBlocking {
        withContext(UserRolesElement(listOf("FREE"))) {
            val result = checkAccess("search_my_cards")
            assertNull(result)
        }
    }

    @Test
    fun `returns error CallToolResult when FREE user accesses PRO tool`() = runBlocking {
        withContext(UserRolesElement(listOf("FREE"))) {
            val result = checkAccess("analyze_tribal_depth")
            assertNotNull(result)
            assertTrue(result.isError == true)
            val text = (result.content.first() as TextContent).text
            assertTrue(text.contains("analyze_tribal_depth"))
        }
    }

    @Test
    fun `returns null when ADMIN accesses PRO tool`() = runBlocking {
        withContext(UserRolesElement(listOf("ADMIN"))) {
            val result = checkAccess("save_deck")
            assertNull(result)
        }
    }
}
