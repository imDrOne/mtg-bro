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
- `httpClient`: Ktor HTTP client for API calls

### Transport Modes

- **stdio**: For Cursor integration (default)
- **http**: Streamable HTTP for Claude web (requires ngrok for public access)

## Environment Variables

| Variable                      | Default                 | Description                           |
|-------------------------------|-------------------------|---------------------------------------|
| `COLLECTION_MANAGER_BASE_URL` | `http://localhost:8080` | collection-manager API URL            |
| `DRAFTSIM_PARSER_BASE_URL`    | `http://localhost:8081` | draftsim-parser API URL               |
| `MCP_TRANSPORT`               | `stdio`                 | Transport mode                        |
| `MCP_HTTP_PORT`               | `3000`                  | HTTP port (when using http transport) |
