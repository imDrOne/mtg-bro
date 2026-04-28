# Централизованные логи через Loki + Grafana

## Summary

Собрать stdout/stderr логи Docker-контейнеров в Loki и смотреть их в Grafana.
Grafana не публикуется наружу: доступ только через SSH tunnel или приватную сеть.
На первом этапе код сервисов менять не нужно: Spring/Ktor уже пишут логи в stdout, Docker их собирает.

## Key Changes

- Добавить в `docker/docker-compose.prod.yml` сервисы:
  - `loki` для хранения логов;
  - `grafana` для UI;
  - `alloy` как агент, который читает Docker container logs и отправляет их в Loki.
- Добавить конфиги в `docker/observability/`:
  - `loki-config.yml`;
  - `alloy-config.alloy`;
  - Grafana provisioning для datasource `Loki`.
- Настроить Docker log rotation для app-контейнеров или daemon-level `local` logging driver, чтобы stdout-логи не разрастались бесконечно.
- Пробросить Grafana только локально:
  - `127.0.0.1:3001:3000`;
  - доступ с машины разработчика: `ssh -L 3001:127.0.0.1:3001 <user>@<server>`, затем `http://localhost:3001`.
- Обновить `docs/deploy.md`:
  - команды запуска observability stack;
  - как открыть SSH tunnel;
  - базовые LogQL-запросы по контейнерам: `collection-manager`, `auth-service`, `mcp-server`, `draftsim-parser`, `wizard-stat-aggregator`.

## Resource Estimate

Для текущего масштаба проекта, примерно 5 app-контейнеров плюс Caddy/Postgres:

| Resource | Realistic | Comfortable |
|---|---:|---:|
| RAM | `500-800 MB` | `1-1.5 GB` |
| CPU | mostly idle, peaks up to `0.5 vCPU` | `1 vCPU` headroom |
| Disk | depends on retention | `10-20 GB` for logs |

Expected component footprint:

- Alloy: usually `50-150 MB RAM`.
- Loki: `200-500 MB RAM` for low-volume logs.
- Grafana: `150-300 MB RAM`.
- CPU is mostly noticeable during active search or high log ingestion.

Disk is the main sizing factor. With `14d` retention:

- Low traffic: `1-3 GB`.
- Normal production with INFO logs: `5-10 GB`.
- Verbose HTTP/client logging: `20+ GB`.

Practical minimum: avoid running Grafana + Loki on a server with less than `2 GB RAM`.
With `4 GB RAM` and `15-20 GB` free disk, this is safe to start.

## Test Plan

- Bring up the observability stack through Docker Compose.
- Check that `docker compose ps` shows `loki`, `grafana`, and `alloy` running.
- Open Grafana through SSH tunnel and verify that the Loki datasource works.
- Generate a request to any service and find the log by container name.
- Verify that `docker compose logs <service>` still works as a fallback.

## Assumptions

- First stage is logs only: no alerts and no metrics.
- Grafana is not exposed through Caddy.
- Start with `7-14d` retention and increase only after measuring disk usage.
- Docker log rotation must be configured so Docker's own log files cannot fill the disk.

## References

- Docker logging configuration: https://docs.docker.com/engine/logging/configure/
- Docker `local` logging driver: https://docs.docker.com/engine/logging/drivers/local/
- Grafana Alloy Docker log collection: https://grafana.com/docs/alloy/latest/monitor/monitor-docker-containers/
