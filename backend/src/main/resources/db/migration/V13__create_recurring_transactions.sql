-- Lançamentos recorrentes: modelo (regra) que gera uma transação real a cada ocorrência vencida.
-- generated_occurrences conta quantas ocorrências já viraram transação — a próxima data é sempre
-- recalculada a partir de start_date (start_date + N períodos), então dia 31 mensal não "escorrega"
-- para 28 depois de passar por fevereiro.
CREATE TABLE recurring_transactions (
    id                    BIGSERIAL PRIMARY KEY,
    user_id               BIGINT          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    account_id            BIGINT          NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    category_id           BIGINT          REFERENCES categories(id) ON DELETE SET NULL,
    client_id             BIGINT          REFERENCES clients(id) ON DELETE SET NULL,
    description           VARCHAR(255)    NOT NULL,
    amount                NUMERIC(14,2)   NOT NULL,
    type                  VARCHAR(20)     NOT NULL,
    frequency             VARCHAR(20)     NOT NULL,
    start_date            DATE            NOT NULL,
    end_date              DATE,
    generated_occurrences INTEGER         NOT NULL DEFAULT 0,
    active                BOOLEAN         NOT NULL DEFAULT true,
    created_at            TIMESTAMP       NOT NULL DEFAULT now()
);

CREATE INDEX idx_recurring_transactions_user ON recurring_transactions(user_id);
CREATE INDEX idx_recurring_transactions_account ON recurring_transactions(account_id);

-- Transação gerada por uma recorrência guarda a origem; excluir a recorrência mantém as
-- transações já lançadas (só desfaz o vínculo).
ALTER TABLE transactions
    ADD COLUMN recurring_transaction_id BIGINT
        REFERENCES recurring_transactions(id) ON DELETE SET NULL;

CREATE INDEX idx_transactions_recurring ON transactions(recurring_transaction_id);
