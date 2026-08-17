-- V19__agenda_event_pending_rollover.sql
-- Marca eventos ONE_TIME que venceram sem serem concluidos, pra disparar o
-- check-in "voce ja fez isso?" dentro do app (AgendaRolloverScheduler +
-- AgendaEventService.confirmRollover).

ALTER TABLE agenda_event
    ADD COLUMN pending_rollover BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_agenda_event_pending_rollover ON agenda_event (account_id, pending_rollover);
