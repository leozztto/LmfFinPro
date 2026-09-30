-- Metas deixam de ser puramente virtuais: aporte/resgate passam a ser transferências reais entre
-- a conta reserva (accountId, pode ser compartilhada por várias metas) e a conta de origem
-- (fundingAccountId). App ainda não está em produção: sem meta real a preservar, limpamos o que
-- houver em vez de inferir uma conta pra linhas antigas sem esse vínculo.
DELETE FROM goal_contributions;
DELETE FROM savings_goals;

ALTER TABLE savings_goals
    ADD COLUMN account_id         BIGINT NOT NULL REFERENCES accounts(id),
    ADD COLUMN funding_account_id BIGINT NOT NULL REFERENCES accounts(id);

CREATE INDEX idx_savings_goals_account ON savings_goals(account_id);
CREATE INDEX idx_savings_goals_funding_account ON savings_goals(funding_account_id);

-- Cada aporte/resgate é a transferência de verdade por trás dele: excluir a transferência (pela
-- tela de Transferências) também remove o registro do aporte na meta.
ALTER TABLE goal_contributions
    ADD COLUMN transfer_id BIGINT NOT NULL REFERENCES transfers(id) ON DELETE CASCADE;

CREATE INDEX idx_goal_contributions_transfer ON goal_contributions(transfer_id);
