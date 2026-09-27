-- Anexos de transação (comprovante de pagamento, nota fiscal, recibo). O arquivo fica no
-- armazenamento de arquivos (FileStoragePort — disco local em volume Docker, trocável por S3);
-- aqui fica só o registro. storage_key é gerado pelo sistema (UUID), nunca o nome enviado.
CREATE TABLE transaction_attachments (
    id             BIGSERIAL     PRIMARY KEY,
    transaction_id BIGINT        NOT NULL REFERENCES transactions(id) ON DELETE CASCADE,
    user_id        BIGINT        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    document_type  VARCHAR(20)   NOT NULL,
    file_name      VARCHAR(255)  NOT NULL,
    content_type   VARCHAR(100)  NOT NULL,
    size_bytes     BIGINT        NOT NULL,
    storage_key    VARCHAR(100)  NOT NULL UNIQUE,
    created_at     TIMESTAMP     NOT NULL DEFAULT now()
);

CREATE INDEX idx_transaction_attachments_transaction ON transaction_attachments(transaction_id);
CREATE INDEX idx_transaction_attachments_user ON transaction_attachments(user_id);
