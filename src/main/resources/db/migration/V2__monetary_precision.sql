-- V2__monetary_precision.sql
-- Reduz precisao de installment_amount de NUMERIC(38,2) para NUMERIC(19,2).
-- NUMERIC(19,2): suporta ate 9.999.999.999.999.999,99 — adequado para valores financeiros pessoais.
-- Esta migration deve ser aplicada antes que Hibernate valide a entidade Bill com precision=19, scale=2.

ALTER TABLE bill
    ALTER COLUMN installment_amount TYPE NUMERIC(19, 2);
