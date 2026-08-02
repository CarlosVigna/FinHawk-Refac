-- V16__agenda.sql
-- Agenda pessoal: eventos pontuais (ONE_TIME) e habitos recorrentes (HABIT).
-- Segue o mesmo padrao de item recorrente + registro de conclusao por
-- periodo ja usado em checklist_item/checklist_completion, mas com
-- granularidade diaria (event_date) em vez de mensal.

CREATE TABLE agenda_event (
    id                    BIGSERIAL PRIMARY KEY,
    title                 VARCHAR(255) NOT NULL,
    description           VARCHAR(1000),
    account_id            BIGINT NOT NULL
        REFERENCES account (id_account) ON DELETE CASCADE,
    type                  VARCHAR(20) NOT NULL
        CHECK (type IN ('ONE_TIME', 'HABIT')),
    event_date_time       TIMESTAMP,
    recurrence_frequency  VARCHAR(20)
        CHECK (recurrence_frequency IN ('DAILY', 'WEEKLY')),
    time_of_day           TIME,
    active                BOOLEAN NOT NULL DEFAULT TRUE,
    reminder_sent_at      TIMESTAMP,
    deleted_at            TIMESTAMP,
    created_at            TIMESTAMP NOT NULL,
    updated_at            TIMESTAMP NOT NULL
);

CREATE TABLE agenda_event_day_of_week (
    agenda_event_id BIGINT NOT NULL
        REFERENCES agenda_event (id) ON DELETE CASCADE,
    day_of_week     VARCHAR(10) NOT NULL
        CHECK (day_of_week IN ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'))
);

CREATE TABLE agenda_event_completion (
    id              BIGSERIAL PRIMARY KEY,
    agenda_event_id BIGINT NOT NULL
        REFERENCES agenda_event (id) ON DELETE CASCADE,
    event_date      DATE NOT NULL,
    status          VARCHAR(10) NOT NULL
        CHECK (status IN ('DONE', 'SKIPPED')),
    completed_at    TIMESTAMP NOT NULL,
    CONSTRAINT uq_agenda_event_completion UNIQUE (agenda_event_id, event_date)
);

-- "o que tem amanha" (eventos pontuais por data/hora)
CREATE INDEX idx_agenda_event_account_datetime ON agenda_event (account_id, event_date_time);
-- habitos ativos
CREATE INDEX idx_agenda_event_account_active ON agenda_event (account_id, active);
CREATE INDEX idx_agenda_event_day_of_week_event_id ON agenda_event_day_of_week (agenda_event_id);
CREATE INDEX idx_agenda_event_completion_event_id ON agenda_event_completion (agenda_event_id);
