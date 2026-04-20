package xyz.candycrawler.mcpserver

import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ListToolsRequest
import io.modelcontextprotocol.kotlin.sdk.types.ListToolsResult
import io.modelcontextprotocol.kotlin.sdk.types.Method
import io.modelcontextprotocol.kotlin.sdk.shared.Transport
import xyz.candycrawler.mcpserver.auth.ToolAccessConfigData
import xyz.candycrawler.mcpserver.auth.currentUserRoles
import xyz.candycrawler.mcpserver.auth.hasAccess
import xyz.candycrawler.mcpserver.auth.isAuthEnabled

/**
 * MCP server subclass that filters the tools/list response based on the current user's roles.
 *
 * FREE users see only the FREE tier tools; PRO and ADMIN users see all tools.
 * When no auth context is present (stdio / local dev), all tools are visible.
 *
 * Note: because [Server.createSession] is not open, per-session filtering of the wire
 * tools/list response is applied by re-setting the ToolsList request handler immediately
 * after [createSession] returns. Call [createFilteredSession] instead of [createSession]
 * from transport setup code to enable this behaviour.
 */
class FilteredMcpServer(
    serverInfo: Implementation,
    options: ServerOptions,
    private val toolAccessConfig: ToolAccessConfigData,
) : Server(serverInfo, options) {

    /**
     * Returns the subset of registered tool names visible to the current user.
     *
     * Reads the user's roles from the coroutine context (set by [UserRolesElement]).
     * Falls back to returning all tool names when auth is disabled (no context element present).
     */
    suspend fun visibleToolNames(): List<String> {
        if (!isAuthEnabled()) return tools.keys.toList()
        val roles = currentUserRoles()
        return tools.keys.filter { toolName -> toolAccessConfig.hasAccess(toolName, roles) }
    }

    /**
     * Creates a session and immediately overrides the tools/list handler so that it
     * filters results according to the caller's coroutine-context roles.
     *
     * Use this instead of [createSession] in HTTP-transport code where the coroutine
     * context already carries a [UserRolesElement].
     */
    suspend fun createFilteredSession(transport: Transport) =
        createSession(transport).also { session ->
            session.setRequestHandler<ListToolsRequest>(Method.Defined.ToolsList) { _, _ ->
                ListToolsResult(
                    tools = visibleToolNames().mapNotNull { name -> tools[name]?.tool },
                    nextCursor = null,
                )
            }
        }
}
