# Suggested Commands

## Build
```bash
./gradlew build                          # all modules
./gradlew jibDockerBuild                 # Docker images (all)
./gradlew :mcp-server:installDist        # MCP server for Cursor
./gradlew runLocal                       # local: postgres + services + ngrok
```

## Test
```bash
./gradlew test                           # all
./gradlew :collection-manager:test
./gradlew :wizard-stat-aggregator:test
./gradlew :draftsim-parser:test
./gradlew :collection-manager:test --tests "xyz.candycrawler.collectionmanager.SomeTest"
```

## Run (local)
```bash
docker compose -f docker/docker-compose.local.yml up postgres -d
./gradlew :collection-manager:bootRun    # profile: local
./gradlew :wizard-stat-aggregator:bootRun
./gradlew :draftsim-parser:bootRun
./mcp-server/build/install/mcp-server/bin/mcp-server --transport stdio
./mcp-server/build/install/mcp-server/bin/mcp-server --transport http --port 3000
```

## Database Migrations
```bash
./gradlew :collection-manager:createMigration -PsqlName=add_new_table
./gradlew :collection-manager:update
# same pattern for wizard-stat-aggregator, draftsim-parser
```

## Deploy
```bash
python3 scripts/deploy.py   # interactive: select modules, auto-bumps patch, git tag+push
```

## System Utils (Darwin)
```bash
git, ls, find, grep, rg (ripgrep preferred for code search)
```
