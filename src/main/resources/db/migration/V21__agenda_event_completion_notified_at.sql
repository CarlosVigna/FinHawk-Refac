-- V21__agenda_event_completion_notified_at.sql
-- Habito concluido deixa de notificar instantaneamente e vira resumo
-- consolidado 4x/dia (AgendaNotificationScheduler.habitCompletionDigest) --
-- notified_at marca quando a conclusao ja entrou num resumo, pra nao
-- repetir no proximo disparo. Nulo = ainda nao reportada.

ALTER TABLE agenda_event_completion
    ADD COLUMN notified_at TIMESTAMP;

CREATE INDEX idx_agenda_event_completion_notified_at ON agenda_event_completion (status, notified_at);
