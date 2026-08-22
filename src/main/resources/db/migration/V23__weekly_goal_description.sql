-- V23__weekly_goal_description.sql
-- Descricao opcional pra meta semanal, mesmo padrao de AgendaEvent.description.

ALTER TABLE weekly_goal ADD COLUMN description VARCHAR(1000) NULL;
