-- liquibase formatted sql

-- changeset Andrey Tikholoz:20260330000001
-- comment: add_mtga_id_to_cards

alter table cards add column mtga_id text;
