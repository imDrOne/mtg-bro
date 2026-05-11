---
name: mtg-bro-deckbuilding
description: Use when building or improving Magic: The Gathering decks with mtg-bro MCP tools, local collection data, Scryfall search, and Draftsim article insights.
---

# MTG Bro Deckbuilding Workflow

Use this guide when helping a user build, tune, or save a Magic: The Gathering deck through mtg-bro MCP.

## Start With Constraints

Clarify or infer the deck goal before searching broadly:

- Format: Standard, Commander, Draft, Sealed, or another supported format.
- Colors or commander, if any.
- Collection-only vs. external recommendations.
- Budget, competitiveness, and preferred play style.
- Required cards, tribes, mechanics, or themes.

## Gather Data Efficiently

Prefer this order unless the user asks for a narrower task:

1. Use `list_scryfall_format_codes` when you need valid format/color query syntax.
2. Use `get_collection_overview` only for broad collection shape before choosing a direction.
3. Use `search_my_cards` with narrow filters for owned cards. Do not load the whole collection unless the user asks for an overview.
4. Use `analyze_tribal_depth` when the user names a creature type or kindred theme.
5. Use `list_draftsim_articles` to discover relevant Draftsim article IDs by title, slug, keywords, or favorite catalog browsing.
6. Use `search_draftsim_articles` for strategy questions, archetypes, mechanics, set reviews, and semantic insight search.
7. Use `get_draftsim_articles` only after selecting specific article IDs.
8. Use `search_limited_card_stats` for Draft and Sealed card performance from 17lands.
9. Use `search_scryfall` for legality checks and external card discovery.

Call the smallest useful tool first. Prefer filtered searches over broad dumps, and fetch full article/card detail only after selecting candidates.

## Format Notes

- Standard: Constructed format. Mainboard must be at least 60 cards. Sideboard is usually up to 15 cards. Use format legality and normal copy limits unless a card says otherwise.
- Sealed: Limited format built from a sealed pool. Main deck must be at least 40 cards. Prioritize curve, fixing, removal, bombs, and coherent two-color builds unless the pool supports splashing. Use `search_limited_card_stats` with `match_type=Sealed`.
- Draft: Limited format built from drafted cards. Main deck must be at least 40 cards. Use Draftsim insights for set archetypes, mechanics, signals, card roles, and synergy density. Use `search_limited_card_stats` with `match_type=QuickDraft`.

## Limited Stats

Use `search_limited_card_stats` when evaluating Draft or Sealed card quality for a known set.

- Required input: `set_code`, for example `dmu`, `eoe`, or `fin`.
- Draft stats: pass `match_type=QuickDraft`.
- Sealed stats: pass `match_type=Sealed`.
- Card grade filter: pass `tiers=A+`, `tiers=A`, `tiers=A-`, `tiers=B+`, etc. to filter by the 17lands Grades view.
- Omit `tiers` for the full result set. `tier=null` means 17lands did not have enough GIH WR data to compute a grade.
- To evaluate known cards, pass exact `names` or `mtga_ids` instead of fetching a whole set.
- To discover strong performers, use `min_win_rate`, `max_win_rate`, `sort=win_rate`, and small `page_size`.
- Useful sort fields: `win_rate`, `game_count`, `drawn_improvement_win_rate`, `name`, `mtga_id`.
- Win rates are decimals: `0.58` means 58%.

Prefer limited stats for empirical card performance and Draftsim articles for context such as archetypes, mechanics, signals, and synergy requirements.

## Build The Deck

- Start from the user's constraints and strongest available synergies.
- Prefer owned cards when the user wants collection-aware suggestions.
- If the user's library is missing enough basic lands, do not contort the deck or search for replacements. Treat basic lands as effectively unlimited unless the user explicitly says lands must come from the tracked collection.
- Use Draftsim insights for Limited archetypes, mechanics, card roles, and draft context.
- Use Scryfall for exact legality, color identity, card text, and replacement options.
- Explain important tradeoffs: curve, mana, removal, threats, card advantage, synergy density, and sideboard needs.

## Before Saving

Before calling `save_deck`:

- Resolve card IDs through `search_my_cards`.
- Check format size rules: Standard mainboard at least 60 cards; Draft and Sealed at least 40 cards.
- Respect copy limits unless the card or format allows exceptions.
- Keep mainboard, sideboard, commander, and maybeboard clearly separated.
- If validation fails, fix the deck and retry instead of saving partial data.
