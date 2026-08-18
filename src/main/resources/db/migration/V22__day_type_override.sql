-- V22__day_type_override.sql
-- Escolha manual do tipo de dia (plantao/folga) por data, com prioridade
-- sobre o calculo automatico de DayTypeService. Um registro por
-- account_id+date (upsert). So PLANTAO/FOLGA -- ENTREGA/FIM_DE_SEMANA
-- nunca e sobrescrito, sempre calculado pelo dia da semana.

CREATE TABLE day_type_override (
    id         BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL
        REFERENCES account (id_account) ON DELETE CASCADE,
    date       DATE NOT NULL,
    day_type   VARCHAR(20) NOT NULL
        CHECK (day_type IN ('PLANTAO', 'FOLGA')),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_day_type_override UNIQUE (account_id, date)
);
