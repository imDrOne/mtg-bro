-- liquibase formatted sql

-- changeset wizard-stat-aggregator:20260503000001
-- comment: add nullable limited stats tier

alter table card_limited_stats
    add column tier varchar(20);
