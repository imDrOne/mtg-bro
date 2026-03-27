-- liquibase formatted sql

-- changeset Andrey Tikholoz:20260326000001
-- comment: create_parse_tasks

create table parse_tasks
(
    id                  uuid         primary key default gen_random_uuid(),
    keyword             text         not null,
    status              varchar(30)  not null,
    total_articles      int,
    processed_articles  int          not null default 0,
    error_message       text,
    created_at          timestamp    not null,
    updated_at          timestamp    not null
);

-- rollback drop table parse_tasks;
