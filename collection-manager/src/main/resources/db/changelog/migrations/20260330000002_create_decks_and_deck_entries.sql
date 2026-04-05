-- liquibase formatted sql

-- changeset Andrey Tikholoz:20260330000002
-- comment: create_decks_and_deck_entries

create table decks
(
    id             bigserial    primary key,
    name           varchar(255) not null,
    format         varchar(20)  not null,
    color_identity text[]       not null,
    comment        text,
    created_at     timestamp    not null default now(),
    updated_at     timestamp    not null default now()
);

create table deck_entries
(
    id           bigserial primary key,
    deck_id      bigint  not null references decks (id) on delete cascade,
    card_id      bigint  not null references cards (id),
    quantity     int     not null check (quantity > 0),
    is_sideboard boolean not null default false,

    constraint uq_deck_entries_deck_card_sideboard unique (deck_id, card_id, is_sideboard)
);

create index idx_deck_entries_deck_id on deck_entries (deck_id);
