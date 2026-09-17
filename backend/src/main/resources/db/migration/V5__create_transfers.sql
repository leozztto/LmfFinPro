-- Transferência entre contas do mesmo usuário: registra a operação "pai" e vincula
-- as duas transações (saída/entrada) geradas a partir dela via transactions.transfer_id.
-- Excluir um transfer cascateia a exclusão das duas transações vinculadas, evitando
-- ter que apagar manualmente as duas pernas em código Java.

CREATE TABLE transfers (
    id                BIGSERIAL PRIMARY KEY,
    user_id           BIGINT              NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    from_account_id   BIGINT              NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    to_account_id     BIGINT              NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    amount            NUMERIC(14,2)       NOT NULL,
    transfer_date     DATE                NOT NULL,
    description       VARCHAR(255),
    created_at        TIMESTAMP           NOT NULL DEFAULT now()
);

ALTER TABLE transactions ADD COLUMN transfer_id BIGINT REFERENCES transfers(id) ON DELETE CASCADE;

CREATE INDEX idx_transfers_user ON transfers(user_id);
CREATE INDEX idx_transactions_transfer ON transactions(transfer_id);
