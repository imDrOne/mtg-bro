-- liquibase formatted sql

-- changeset Andrey Tikholoz:20260308000000
-- comment: create_cards_and_collection_entries

create table cards
(
    id                   bigserial primary key,
    scryfall_id          uuid             not null,
    oracle_id            uuid             not null,
    name                 varchar(255)     not null,
    lang                 varchar(10)      not null default 'en',
    layout               varchar(50)      not null,
    mana_cost            varchar(100),
    cmc                  double precision not null,
    type_line            varchar(255)     not null,
    oracle_text          text,
    colors               text[]           not null,
    color_identity       text[]           not null,
    keywords             text[]           not null,
    power                varchar(10),
    toughness            varchar(10),
    loyalty              varchar(10),
    set_code             varchar(10)      not null,
    set_name             varchar(255)     not null,
    collector_number     varchar(20)      not null,
    rarity               varchar(20)      not null,
    released_at          date,
    image_uri_small      text,
    image_uri_normal     text,
    image_uri_large      text,
    image_uri_png        text,
    image_uri_art_crop   text,
    image_uri_border_crop text,
    price_usd            varchar(20),
    price_usd_foil       varchar(20),
    price_eur            varchar(20),
    price_eur_foil       varchar(20),
    flavor_text          text,
    artist               varchar(255),

    constraint uq_cards_scryfall_id unique (scryfall_id),
    constraint uq_cards_set_collector_lang unique (set_code, collector_number, lang)
);

create table collection_entries
(
    id         bigserial primary key,
    card_id    bigint    not null references cards (id),
    quantity   int       not null check (quantity > 0),
    created_at timestamp not null default now(),
    updated_at timestamp not null default now(),

    constraint uq_collection_entries_card_id unique (card_id)
);
