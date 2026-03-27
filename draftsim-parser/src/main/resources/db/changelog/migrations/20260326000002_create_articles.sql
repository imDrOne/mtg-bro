-- liquibase formatted sql

-- changeset Andrey Tikholoz:20260326000002
-- comment: create_articles

create table articles
(
    id           bigserial    primary key,
    external_id  bigint       not null,
    title        text         not null,
    slug         varchar(500) not null,
    url          text         not null,
    html_content text,
    text_content text,
    published_at timestamp,
    fetched_at   timestamp,

    constraint uq_articles_external_id unique (external_id)
);

-- rollback drop table articles;
