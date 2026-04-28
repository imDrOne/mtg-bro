# Wizard Stat Aggregator: tracked sets for scheduled parsing

## Summary

Add a persistent admin-managed list of Magic set codes that `wizard-stat-aggregator`
should parse on schedule. Each tracked set has a `watch_until` date; scheduler runs
only for sets whose date has not expired.

## Key Changes

- Add a `tracked_limited_stat_sets` table in `wizard-stat-aggregator` with `set_code`,
  `watch_until`, `created_at`, and `updated_at`.
- Treat `watch_until` as inclusive: a set with `watch_until = today` is still active.
- Add domain, repository, Exposed table/mapper, and service code for listing, upserting,
  deleting, and finding active tracked sets.
- Update `CardLimitedStatsScheduler` to read active sets from the database instead of
  parsing one configured `set-code`.
- Add admin CRUD endpoints under `/api/v1/card-limited-stats/tracked-sets`.
- Restrict all `wizard-stat-aggregator` API endpoints to `ROLE_ADMIN`.
- Add permission `api:stats:tracked-sets:manage` in `auth-service` and grant it to
  `ADMIN` by default.

## Test Plan

- Repository and mapper tests cover insert, update, delete, list, and active-date logic.
- Scheduler tests cover multiple active sets, expired sets, empty list, and per-set failures.
- Security tests cover unauthenticated requests, non-admin JWTs, admin JWTs without the
  permission, and admin JWTs with the permission.
- Run `./gradlew :wizard-stat-aggregator:test :auth-service:test`.

## Assumptions

- Admin-only API access is enforced by role first; permissions remain separate method-level
  guards for specific actions.
- Existing manual collection permission `api:stats:collect` remains unchanged.
- `scheduler.card-limited-stats.enabled` and `cron` remain; `set-code` is deprecated and
  removed from scheduler usage.
