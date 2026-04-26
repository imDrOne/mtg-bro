-- liquibase formatted sql

-- changeset Andrey Tikholoz:20260425000002
-- comment: make user_id NOT NULL, adjust unique constraints and add indexes

alter table collection_entries alter column user_id set not null;
alter table decks              alter column user_id set not null;
alter table deck_entries       alter column user_id set not null;

-- collection uniqueness now per (user, card, foil) — same user cannot own the same card+foil twice
alter table collection_entries drop constraint uq_collection_entries_card_id_foil;
alter table collection_entries
    add constraint uq_collection_entries_user_card_foil unique (user_id, card_id, foil);

create index idx_collection_entries_user on collection_entries (user_id);
create index idx_decks_user_id           on decks              (user_id);
create index idx_deck_entries_user       on deck_entries       (user_id);
