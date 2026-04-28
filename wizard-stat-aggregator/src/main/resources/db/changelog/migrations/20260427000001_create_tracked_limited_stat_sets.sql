-- liquibase formatted sql

-- changeset wizard-stat-aggregator:20260427000001
-- comment: create_tracked_limited_stat_sets

CREATE TABLE tracked_limited_stat_sets
(
    set_code    VARCHAR(10) PRIMARY KEY,
    watch_until DATE        NOT NULL,
    created_at TIMESTAMP   NOT NULL DEFAULT now(),
    updated_at TIMESTAMP   NOT NULL DEFAULT now()
);

