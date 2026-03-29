-- liquibase formatted sql

-- changeset Andrey Tikholoz:20260329000005
-- comment: extend_articles_with_favorite_and_analysis_tracking

alter table articles
    add column favorite          boolean   not null default false,
    add column error_msg         text,
    add column analyz_started_at timestamp,
    add column analyz_end_at     timestamp;
