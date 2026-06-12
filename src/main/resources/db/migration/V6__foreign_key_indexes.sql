-- V6__foreign_key_indexes.sql
-- Cria indexes nas colunas FK para otimizar JOINs e lookups frequentes.
-- PostgreSQL nao cria indexes automaticamente em colunas FK (ao contrario do MySQL).
-- IF NOT EXISTS garante idempotencia.
-- Pre-condicao validada em 12/06/2026: nenhum dos indexes existia previamente.

CREATE INDEX IF NOT EXISTS idx_bill_account_id
    ON bill (account_id);

CREATE INDEX IF NOT EXISTS idx_bill_category_id
    ON bill (category_id);

CREATE INDEX IF NOT EXISTS idx_category_account_id
    ON category (account_id);

CREATE INDEX IF NOT EXISTS idx_checklist_item_account_id
    ON checklist_item (account_id);

CREATE INDEX IF NOT EXISTS idx_account_id_user
    ON account (id_user);
