# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

MCP (Model Context Protocol) server for MTG deck building. Exposes tools for Claude (Cursor/web) to search cards, view collections, and assist with deck building. This is a non-Spring Kotlin module using Ktor + MCP SDK.

## Build Commands

```bash
# Build for Cursor integration (stdio transport)
./gradlew :mcp-server:installDist

# Build Docker image
./gradlew :mcp-server:jibDockerBuild

# Run with stdio transport
./mcp-server/build/install/mcp-server/bin/mcp-server --transport stdio

# Run with HTTP transport (for Claude web via ngrok)
./mcp-server/build/install/mcp-server/bin/mcp-server --transport http --port 3000
```

## Architecture

### Tool Pattern

Each MCP tool consists of two functions in a `*Handler.kt` file:
1. `*Schema()` - Returns `ToolSchema` defining parameters with JSON Schema
2. `handle*()` - Async handler that calls collection-manager API and returns `CallToolResult`

Tools are registered in `McpServer.kt` via `server.addTool()`.

### Adding a New Tool

1. Create `tools/NewToolHandler.kt` with schema and handler functions
2. Register in `McpServer.kt`:
```kotlin
server.addTool(
    name = "tool_name",
    description = "Tool description for Claude",
    inputSchema = newToolSchema(),
) { request -> handleNewTool(context, request) }
```

### ToolContext

Shared context passed to all handlers containing:
- `baseUrl`: collection-manager base URL
- `draftsimParserBaseUrl`: draftsim-parser base URL
- `httpClient`: Ktor HTTP client for API calls
- `draftsimSearchConfig`: Draftsim semantic search thresholds

### Resources

The server also exposes the deckbuilding workflow guide as a Markdown resource:
- URI: `mtg-bro://guides/deckbuilding-skill.md`
- Same content is available through `get_deckbuilding_guide` for clients that prefer tools.

### Transport Modes

- **stdio**: For Cursor integration (default, no auth)
- **http**: Streamable HTTP for Claude web (OAuth 2.1 when `AUTH_ISSUER_URI` is set)

### OAuth 2.1 Authorization (HTTP transport)

When `AUTH_ISSUER_URI` and `MCP_BASE_URL` are set, the MCP server acts as an OAuth 2.1 Resource Server (RFC 9728):

1. Unauthenticated requests get `HTTP 401` with `WWW-Authenticate` header
2. Claude discovers auth via `GET /.well-known/oauth-protected-resource` on the MCP server
3. Claude performs Authorization Code + PKCE flow against auth-service
4. Subsequent requests include `Authorization: Bearer <token>`
5. MCP server validates JWT using auth-service's JWKS

Key files:
- `auth/McpAuthPlugin.kt` — Ktor plugin for JWT validation
- `auth/OAuthMetadataRoute.kt` — Protected Resource Metadata endpoint (RFC 9728)

Auth is **disabled** when env vars are not set (local dev, stdio transport).

### Claude Web Connector Setup

In Claude Web: Settings → Connectors → Add custom connector:
- **Name**: mtg-bro
- **Remote MCP server URL**: `https://{MCP_DOMAIN}/mcp`

No OAuth Client ID or Client Secret needed — auth-service supports Dynamic Client
Registration (RFC 7591). Claude.ai automatically registers an OAuth client on first
connection.

If DCR doesn't work (fallback, manual configuration):
- **OAuth Client ID**: `mcp-client`
- **OAuth Client Secret**: raw secret matching the BCrypt hash in auth-service DB

## Environment Variables

| Variable                      | Default                 | Description                            |
|-------------------------------|-------------------------|----------------------------------------|
| `COLLECTION_MANAGER_BASE_URL` | `http://localhost:8080` | collection-manager API URL             |
| `DRAFTSIM_PARSER_BASE_URL`    | `http://localhost:8081` | draftsim-parser API URL                |
| `DRAFTSIM_SEMANTIC_SIMILARITY_THRESHOLDS` | `0.80,0.65,0.50` | Ordered CSV thresholds for Draftsim semantic retry |
| `MCP_TRANSPORT`               | `stdio`                 | Transport mode                         |
| `MCP_HTTP_PORT`               | `3000`                  | HTTP port (when using http transport)  |
| `AUTH_ISSUER_URI`             | _(none)_                | Auth-service public URL; enables OAuth |
| `MCP_BASE_URL`                | _(none)_                | MCP server public URL; enables OAuth   |
