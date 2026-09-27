-- Patrimônio líquido: valor de mercado das contas de investimento e dívidas com saldo devedor.
-- Nada de saldo persistido: o patrimônio é calculado a cada leitura a partir destes registros.

-- Valor de mercado de uma conta de investimento no fim do dia (ex.: saldo do extrato da
-- corretora). Um por conta e dia; o saldo da conta passa a ser o último valor informado mais as
-- movimentações posteriores a ele.
CREATE TABLE account_valuations (
    id             BIGSERIAL      PRIMARY KEY,
    account_id     BIGINT         NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    valuation_date DATE           NOT NULL,
    value          NUMERIC(14,2)  NOT NULL,
    created_at     TIMESTAMP      NOT NULL DEFAULT now(),
    CONSTRAINT uk_account_valuations_account_date UNIQUE (account_id, valuation_date)
);

-- Dívidas acompanhadas no patrimônio. As parcelas continuam sendo despesas comuns.
CREATE TABLE debts (
    id         BIGSERIAL     PRIMARY KEY,
    user_id    BIGINT        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name       VARCHAR(100)  NOT NULL,
    type       VARCHAR(20)   NOT NULL,
    creditor   VARCHAR(100),
    created_at TIMESTAMP     NOT NULL DEFAULT now()
);

CREATE INDEX idx_debts_user ON debts(user_id);

-- Saldo devedor numa data; vale até o próximo informado (zero = quitada). Um por dívida e dia.
CREATE TABLE debt_balances (
    id           BIGSERIAL      PRIMARY KEY,
    debt_id      BIGINT         NOT NULL REFERENCES debts(id) ON DELETE CASCADE,
    balance_date DATE           NOT NULL,
    balance      NUMERIC(14,2)  NOT NULL,
    created_at   TIMESTAMP      NOT NULL DEFAULT now(),
    CONSTRAINT uk_debt_balances_debt_date UNIQUE (debt_id, balance_date)
);
