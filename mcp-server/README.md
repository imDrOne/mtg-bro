# mtg-bro MCP Server

MCP (Model Context Protocol) server for deck building. Exposes tools so Claude (in Cursor and web) can search cards, view your collection, and suggest decks based on format, colors, and specific cards.

## Prerequisites

1. **collection-manager** must be running on port 8080 (or set `COLLECTION_MANAGER_BASE_URL`).
2. Build the MCP server: `./gradlew :mcp-server:installDist`

## Tools

| Tool                         | Description                                                                                                                                               |
|------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------|
| `search_my_cards`            | Search cards in your library with filters (colors, color_identity, type, set, rarity). Use filters from user preferences to avoid loading large datasets. |
| `search_scryfall`            | Search Scryfall card database. Use Scryfall syntax: `f:standard`, `c:bg`, `t:creature`, etc.                                                              |
| `get_card`                   | Get a single card from your library by set code and collector number.                                                                                     |
| `list_scryfall_format_codes` | Returns format and color codes for both search tools.                                                                                                     |

## Cursor (stdio)

1. Build: `./gradlew :mcp-server:installDist`
2. Add to `.cursor/mcp.json` (replace the path with your project's absolute path):

```json
{
  "mcpServers": {
    "mtg-bro": {
      "command": "/absolute/path/to/mtg-bro/mcp-server/build/install/mcp-server/bin/mcp-server",
      "args": ["--transport", "stdio"],
      "env": {
        "COLLECTION_MANAGER_BASE_URL": "http://localhost:8080"
      }
    }
  }
}
```

3. Restart Cursor. The MCP server will start when you use the chat.

## Claude web (Streamable HTTP) — quick start

The easiest way to run everything locally with a Cloudflare tunnel:

```bash
./gradlew runLocal
```

This single command:
1. Builds `collection-manager` (bootJar) and `mcp-server` (installDist).
2. Starts **collection-manager** on port 8080.
3. Starts **mcp-server** in HTTP mode on port 3000.
4. Starts **cloudflared** tunnel and prints the public URL.

Override ports with Gradle properties: `-PcmPort=9090 -PmcpPort=4000`.

Requires `cloudflared` (`brew install cloudflared`).

Copy the printed tunnel URL (e.g. `https://xxxx.trycloudflare.com/mcp`) into Claude: Settings → Connectors → Add custom connector.

Press Ctrl+C to stop all processes.

> **Note:** ngrok free tier is not suitable — its interstitial page blocks requests from Claude's servers.

### Manual start (without runLocal)

1. Start the MCP server in HTTP mode:

```bash
COLLECTION_MANAGER_BASE_URL=http://localhost:8080 \
  ./mcp-server/build/install/mcp-server/bin/mcp-server --transport http --port 3000
```

The server listens on `http://localhost:3000/mcp`.

2. For Claude web, the server must be reachable from the internet:

```bash
cloudflared tunnel --url http://localhost:3000 --no-autoupdate
```

3. In Claude: Settings → Connectors → Add custom connector → enter the URL (e.g. `https://xxxx.trycloudflare.com/mcp`).

## Configuration

| Env var | Default | Description |
|--------|---------|-------------|
| `COLLECTION_MANAGER_BASE_URL` | `http://localhost:8080` | Base URL of collection-manager |
| `MCP_TRANSPORT` | `stdio` | `stdio` or `http` |
| `MCP_HTTP_PORT` | `3000` | Port for HTTP transport |

## Command-line arguments

- `--transport stdio` — Use stdio transport (for Cursor)
- `--transport http` — Use Streamable HTTP transport
- `--port 3000` — HTTP port (with `--transport http`)
