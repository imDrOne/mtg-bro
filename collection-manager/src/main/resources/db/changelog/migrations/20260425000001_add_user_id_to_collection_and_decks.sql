-- liquibase formatted sql

-- changeset Andrey Tikholoz:20260425000001
-- comment: add nullable user_id to collection_entries, decks, deck_entries with mock backfill

alter table collection_entries add column user_id bigint;
alter table decks              add column user_id bigint;
alter table deck_entries       add column user_id bigint;

-- TODO(Andrey): replace mock user_id (1) with real id from auth_service_db.users before applying
update collection_entries set user_id = 1 where user_id is null;
update decks              set user_id = 1 where user_id is null;
update deck_entries       set user_id = 1 where user_id is null;
