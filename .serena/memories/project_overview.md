# MTG-Bro Project Overview

MTG deck-building assistant. Multi-module Kotlin/Gradle monorepo.

## Modules

| Module | Stack | DB | Purpose |
|--------|-------|----|---------|
| `collection-manager` | Spring Boot + Exposed | `collection_manager_db` | Card collection CRUD, Scryfall proxy, TCGPlayer/Moxfield import |
| `draftsim-parser` | Spring Boot + Exposed | `draftsim_parser_db` | Claude LLM parses Draftsim articles |
| `wizard-stat-aggregator` | Spring Boot + Exposed | `wizard_stat_db` | 17lands.com stats aggregation |
| `auth-service` | Spring Boot + Spring Authorization Server | `auth_service_db` | OAuth 2.0/OIDC server, user registration/login |
| `mcp-server` | Ktor + MCP SDK (no Spring) | — | MCP tools for Claude: search cards, deck building |

## Key Infrastructure
- PostgreSQL (each service has own DB)
- Docker Compose for local dev and prod
- GitHub Actions for CI/CD (tag-driven deployment)
- Liquibase migrations (SQL-based)

## Package Root
`xyz.candycrawler.*`
