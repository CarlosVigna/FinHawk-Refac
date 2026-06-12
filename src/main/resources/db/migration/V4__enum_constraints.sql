-- V4__enum_constraints.sql
-- Adiciona CHECK constraints para colunas de enum, impedindo valores invalidos diretamente no banco.
-- Pre-condicao validada em 12/06/2026: zero registros com valores fora do conjunto permitido.

ALTER TABLE user_account
    ADD CONSTRAINT chk_user_account_role
    CHECK (role IN ('ADMIN', 'VIEWER'));

ALTER TABLE category
    ADD CONSTRAINT chk_category_type
    CHECK (type IN ('RECEIPT', 'PAYMENT'));

ALTER TABLE bill
    ADD CONSTRAINT chk_bill_status
    CHECK (status IN ('PENDING', 'RECEIVED', 'PAID'));

ALTER TABLE bill
    ADD CONSTRAINT chk_bill_periodicity
    CHECK (periodicity IN ('MONTHLY', 'BIMONTHLY', 'QUARTERLY', 'SEMIANNUAL', 'ANNUAL'));
