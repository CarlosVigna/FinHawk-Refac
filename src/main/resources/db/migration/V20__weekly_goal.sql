-- V20__weekly_goal.sql
-- Metas semanais: so titulo + feito/nao-feito, sem contador. weekStartDate
-- (segunda-feira) marca a que semana pertence -- "semana atual" e so filtro
-- por esse valor. WeeklyGoalScheduler recria as nao concluidas na semana
-- seguinte, sem apagar a antiga (fica de historico).

CREATE TABLE weekly_goal (
    id               BIGSERIAL PRIMARY KEY,
    title            VARCHAR(255) NOT NULL,
    account_id       BIGINT NOT NULL
        REFERENCES account (id_account) ON DELETE CASCADE,
    week_start_date  DATE NOT NULL,
    completed        BOOLEAN NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMP NOT NULL,
    updated_at       TIMESTAMP NOT NULL
);

CREATE INDEX idx_weekly_goal_account_week ON weekly_goal (account_id, week_start_date);
CREATE INDEX idx_weekly_goal_week_completed ON weekly_goal (week_start_date, completed);
