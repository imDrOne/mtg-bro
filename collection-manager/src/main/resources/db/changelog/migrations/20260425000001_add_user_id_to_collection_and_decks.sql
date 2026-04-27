-- liquibase formatted sql

-- changeset Andrey Tikholoz:20260425000001
-- comment: add nullable user_id to collection_entries, decks, deck_entries with mock backfill

alter table collection_entries add column user_id bigint;
alter table decks              add column user_id bigint;
alter table deck_entries       add column user_id bigint;

update collection_entries set user_id = 2 where user_id is null;
update decks              set user_id = 2 where user_id is null;
update deck_entries       set user_id = 2 where user_id is null;
