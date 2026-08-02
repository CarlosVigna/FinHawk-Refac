-- V17__notification_log.sql
-- Registro de idempotencia para os jobs de notificacao via WhatsApp
-- (resumo noturno, resumo semanal, aviso matinal de vencimento).
-- A unique constraint garante que o mesmo job nao envie a mesma
-- notificacao duas vezes pra mesma data de referencia, mesmo que o
-- scheduler seja disparado de novo (ex: restart do servico).

CREATE TABLE notification_log (
    id             BIGSERIAL PRIMARY KEY,
    job_key        VARCHAR(50) NOT NULL,
    reference_date DATE        NOT NULL,
    sent_at        TIMESTAMP   NOT NULL,
    CONSTRAINT uq_notification_log UNIQUE (job_key, reference_date)
);
