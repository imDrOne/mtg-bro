-- liquibase formatted sql

-- changeset Andrey Tikholoz:20260326000003
-- comment: create_parse_task_articles

create table parse_task_articles
(
    parse_task_id uuid   not null references parse_tasks (id),
    article_id    bigint not null references articles (id),

    constraint pk_parse_task_articles primary key (parse_task_id, article_id)
);

-- rollback drop table parse_task_articles;
