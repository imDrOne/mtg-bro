-- liquibase formatted sql

-- changeset Andrey Tikholoz:20260428000006
-- comment: add_keywords_to_articles

alter table articles
    add column keywords text[] not null default '{}';

-- rollback alter table articles drop column keywords;
