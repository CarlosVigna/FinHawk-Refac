-- V13__drop_user_plan.sql
-- Remove a coluna plan de user_account, adicionada na V10 para o
-- sistema de planos/limites (Stripe/PlanLimit), ja removido do backend
-- (checkpoint "remove Stripe/PlanLimit, single-tenant"). Nada le ou
-- escreve esse valor -- confirmado via grep em backend e frontend.

ALTER TABLE user_account DROP COLUMN IF EXISTS plan;
