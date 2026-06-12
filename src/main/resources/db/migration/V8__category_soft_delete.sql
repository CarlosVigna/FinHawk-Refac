-- V8__category_soft_delete.sql
-- Adiciona coluna deleted_at em category para suporte a soft delete.
-- NULL = ativa. Preenchida = arquivada (excluída logicamente).
-- Registros existentes mantidos com deleted_at = NULL (ativas).

ALTER TABLE category ADD COLUMN deleted_at TIMESTAMP NULL;
