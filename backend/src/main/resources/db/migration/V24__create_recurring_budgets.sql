-- Orçamentos recorrentes: modelo (regra) que gera um orçamento real (budgets) a cada mês vencido.
-- generated_months conta quantos meses já viraram orçamento — o próximo mês é sempre recalculado a
-- partir de start_month (start_month + N meses), mesma convenção de recurring_transactions.
CREATE TABLE recurring_budgets (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category_id      BIGINT          NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
    limit_value      NUMERIC(14,2)   NOT NULL,
    start_month      DATE            NOT NULL,
    end_month        DATE,
    generated_months INTEGER         NOT NULL DEFAULT 0,
    active           BOOLEAN         NOT NULL DEFAULT true,
    created_at       TIMESTAMP       NOT NULL DEFAULT now()
);

CREATE INDEX idx_recurring_budgets_user ON recurring_budgets(user_id);
