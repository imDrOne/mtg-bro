-- liquibase formatted sql

-- changeset Andrey Tikholoz:20260309185946
-- comment: add_foil_to_collection_entries

alter table collection_entries add column foil boolean not null default false;

alter table collection_entries drop constraint uq_collection_entries_card_id;
alter table collection_entries add constraint uq_collection_entries_card_id_foil unique (card_id, foil);
