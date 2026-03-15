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

The easiest way to run everything locally with Docker and ngrok:

```bash
./gradlew runLocal
```

This single command:
1. Builds Docker images via JIB (`mtg-bro/collection-manager`, `mtg-bro/mcp-server`).
2. Starts **postgres**, **collection-manager**, **mcp-server**, **ngrok** in Docker.

Requires: Docker. Set `NGROK_AUTHTOKEN` (env or `docker/.env` from `docker/.env.example`).

If run from IDE fails with "Cannot run program docker", use `./runLocal.sh` from terminal instead.

Copy the printed tunnel URL (e.g. `https://xxxx.ngrok-free.app/mcp`) into Claude: Settings → Connectors → Add custom connector.

Press Ctrl+C to stop all processes (containers are torn down automatically).

### Manual start (without runLocal)

**Option A — Docker Compose**

```bash
# Set NGROK_AUTHTOKEN in docker/.env or env
./gradlew jibDockerBuild
docker compose -f docker/docker-compose.local.yml up -d
```

**Option B — Local processes**

1. Start collection-manager and postgres (or use `docker compose -f docker/docker-compose.local.yml up -d postgres collection-manager`).
2. Start the MCP server:

```bash
COLLECTION_MANAGER_BASE_URL=http://localhost:8080 \
  ./mcp-server/build/install/mcp-server/bin/mcp-server --transport http --port 3000
```

3. For Claude web: `ngrok http 3000`, then add the URL in Claude: Settings → Connectors.

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
