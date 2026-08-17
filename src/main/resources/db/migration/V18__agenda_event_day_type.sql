-- V18__agenda_event_day_type.sql
-- Etiquetas de tipo de dia (plantao/folga/entrega/fim de semana) que um
-- habito pode ter, alternativa a recurrence_frequency/agenda_event_day_of_week
-- (ver DayTypeService). Mesmo padrao de agenda_event_day_of_week: tabela de
-- colecao simples, sem PK propria.

CREATE TABLE agenda_event_day_type (
    agenda_event_id BIGINT NOT NULL
        REFERENCES agenda_event (id) ON DELETE CASCADE,
    day_type        VARCHAR(20) NOT NULL
        CHECK (day_type IN ('PLANTAO', 'FOLGA', 'ENTREGA', 'FIM_DE_SEMANA'))
);

CREATE INDEX idx_agenda_event_day_type_event_id ON agenda_event_day_type (agenda_event_id);
