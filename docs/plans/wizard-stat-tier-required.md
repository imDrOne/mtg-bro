# Wizard Stat Aggregator: make limited stats tier required

## Summary

After nullable `tier` has been deployed and the scheduler has refreshed tracked sets,
make the 17lands card grade required across storage, API, and MCP output.

## Key Changes

- Decide how to represent insufficient 17lands data before the `not null` change:
  either backfill `tier = 'UNKNOWN'` where GIH WR cannot produce a grade, or keep
  `tier` nullable permanently for sparse rows.
- Change `card_limited_stats.tier` to `not null`.
- Update domain, repository records, REST DTOs, MCP formatting, and tests so `tier`
  is non-null everywhere if the sentinel path is chosen.
- Keep API compatibility: omitted `tiers` must continue to return all cards, while
  explicit `tiers` filters only the requested grades (`A+` through `F`, plus the
  sparse-data sentinel if introduced).

## Test Plan

- Migration test or manual DB check verifies there are no null `tier` rows before
  adding `not null`.
- Repository tests cover updating the grade on the existing
  `(mtga_id, set_code, match_type)` row.
- REST and MCP tests confirm omitted `tiers` returns all grades and explicit
  `tiers=A+` filters to only A+ rows.
- Run `./gradlew :wizard-stat-aggregator:test :mcp-server:test`.

## Assumptions

- Nullable `tier` has already been deployed long enough for scheduled refresh to fill
  tracked sets.
- `tier` is the 17lands card grade computed from the default Grades metric
  (`ever_drawn_win_rate` / GIH WR), not the 17lands `user_group` request parameter.
