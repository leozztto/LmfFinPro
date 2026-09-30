-- Aporte automático de metas: quando ligado, SavingsGoalContributionScheduler aplica a sugestão
-- ("separar com 1 clique") uma vez por dia, sem o usuário precisar clicar.
ALTER TABLE savings_goals ADD COLUMN auto_contribute BOOLEAN NOT NULL DEFAULT false;
