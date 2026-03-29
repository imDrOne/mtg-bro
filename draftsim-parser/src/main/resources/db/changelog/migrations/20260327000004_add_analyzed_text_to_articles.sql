-- liquibase formatted sql

-- changeset Andrey Tikholoz:20260327000004
-- comment: add_analyzed_text_to_articles

alter table articles add column analyzed_text text;

-- rollback alter table articles drop column analyzed_text;
