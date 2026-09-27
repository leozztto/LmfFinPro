-- Tags: classificação livre e transversal às categorias (ex.: #projeto-site-acme, #dedutível).
-- O nome é gravado já normalizado (minúsculas, sem "#", espaços viram "-"), por isso o índice
-- único simples já impede "Acme" e "acme" duplicadas para o mesmo usuário.
CREATE TABLE tags (
    id          BIGSERIAL    PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name        VARCHAR(40)  NOT NULL,
    color       VARCHAR(7),
    created_at  TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_tags_user_name ON tags(user_id, name);

-- Excluir a transação ou a tag remove só o vínculo.
CREATE TABLE transaction_tags (
    transaction_id  BIGINT  NOT NULL REFERENCES transactions(id) ON DELETE CASCADE,
    tag_id          BIGINT  NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
    PRIMARY KEY (transaction_id, tag_id)
);

CREATE INDEX idx_transaction_tags_tag ON transaction_tags(tag_id);

-- Tags do modelo de recorrência, copiadas para cada transação que ele gera.
CREATE TABLE recurring_transaction_tags (
    recurring_transaction_id  BIGINT  NOT NULL REFERENCES recurring_transactions(id) ON DELETE CASCADE,
    tag_id                    BIGINT  NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
    PRIMARY KEY (recurring_transaction_id, tag_id)
);

CREATE INDEX idx_recurring_transaction_tags_tag ON recurring_transaction_tags(tag_id);
