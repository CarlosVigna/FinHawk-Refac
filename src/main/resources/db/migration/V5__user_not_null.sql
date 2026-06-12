-- V5__user_not_null.sql
-- Adiciona NOT NULL nas colunas user_account.email e user_account.password.
-- Alinha o banco com as restricoes de negocio (email e senha sao obrigatorios para autenticacao JWT).
-- Pre-condicao validada em 12/06/2026: 4 usuarios, 0 null_emails, 0 null_passwords.

ALTER TABLE user_account ALTER COLUMN email    SET NOT NULL;
ALTER TABLE user_account ALTER COLUMN password SET NOT NULL;
