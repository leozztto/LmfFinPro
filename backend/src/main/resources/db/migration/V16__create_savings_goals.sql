-- Metas de economia ("caixinhas"): valor-alvo, prazo opcional e, se quiser, um percentual das
-- receitas a separar (income_rate como fração, ex.: 0.06 = 6%, mesma convenção de
-- tax_estimates.applied_rate). O valor guardado não é persistido: é a soma dos aportes.
CREATE TABLE savings_goals (
    id            BIGSERIAL      PRIMARY KEY,
    user_id       BIGINT         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name          VARCHAR(100)   NOT NULL,
    type          VARCHAR(30)    NOT NULL,
    target_amount NUMERIC(14,2)  NOT NULL,
    deadline      DATE,
    income_rate   NUMERIC(6,4),
    created_at    TIMESTAMP      NOT NULL DEFAULT now()
);

CREATE INDEX idx_savings_goals_user ON savings_goals(user_id);

-- Aportes e resgates virtuais: só registram o dinheiro "separado" na meta, sem gerar transação
-- nem mexer no saldo das contas.
CREATE TABLE goal_contributions (
    id                BIGSERIAL      PRIMARY KEY,
    goal_id           BIGINT         NOT NULL REFERENCES savings_goals(id) ON DELETE CASCADE,
    type              VARCHAR(20)    NOT NULL,
    amount            NUMERIC(14,2)  NOT NULL,
    contribution_date DATE           NOT NULL,
    note              VARCHAR(255),
    created_at        TIMESTAMP      NOT NULL DEFAULT now()
);

CREATE INDEX idx_goal_contributions_goal ON goal_contributions(goal_id);
