-- FinPro - schema inicial (MVP)

CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(150)        NOT NULL,
    email           VARCHAR(150)        NOT NULL UNIQUE,
    password_hash   VARCHAR(255)        NOT NULL,
    tax_regime      VARCHAR(50),
    created_at      TIMESTAMP           NOT NULL DEFAULT now()
);

CREATE TABLE accounts (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT              NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name            VARCHAR(150)        NOT NULL,
    type            VARCHAR(30)         NOT NULL,
    initial_balance NUMERIC(14,2)       NOT NULL DEFAULT 0,
    created_at      TIMESTAMP           NOT NULL DEFAULT now()
);

CREATE TABLE categories (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT              REFERENCES users(id) ON DELETE CASCADE,
    name            VARCHAR(100)        NOT NULL,
    type            VARCHAR(20)         NOT NULL,
    color           VARCHAR(20),
    icon            VARCHAR(50)
);

CREATE TABLE category_rules (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT              NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    pattern         VARCHAR(255)        NOT NULL,
    category_id     BIGINT              NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
    weight          INT                 NOT NULL DEFAULT 1
);

CREATE TABLE clients (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT              NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name            VARCHAR(150)        NOT NULL,
    active          BOOLEAN             NOT NULL DEFAULT true
);

CREATE TABLE import_batches (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT              NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    account_id      BIGINT              NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    original_file   VARCHAR(255),
    format          VARCHAR(10)         NOT NULL,
    imported_at     TIMESTAMP           NOT NULL DEFAULT now(),
    status          VARCHAR(20)         NOT NULL
);

CREATE TABLE transactions (
    id              BIGSERIAL PRIMARY KEY,
    account_id      BIGINT              NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    category_id     BIGINT              REFERENCES categories(id) ON DELETE SET NULL,
    client_id       BIGINT              REFERENCES clients(id) ON DELETE SET NULL,
    import_batch_id BIGINT              REFERENCES import_batches(id) ON DELETE SET NULL,
    description     VARCHAR(255)        NOT NULL,
    amount          NUMERIC(14,2)       NOT NULL,
    transaction_date DATE               NOT NULL,
    type            VARCHAR(20)         NOT NULL,
    origin          VARCHAR(20)         NOT NULL DEFAULT 'MANUAL',
    created_at      TIMESTAMP           NOT NULL DEFAULT now()
);

CREATE TABLE tax_estimates (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT              NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reference_month DATE                NOT NULL,
    gross_revenue   NUMERIC(14,2)       NOT NULL,
    applied_rate    NUMERIC(6,4)        NOT NULL,
    estimated_value NUMERIC(14,2)       NOT NULL
);

CREATE TABLE budgets (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT              NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category_id     BIGINT              NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
    reference_month DATE                NOT NULL,
    limit_value     NUMERIC(14,2)       NOT NULL
);

CREATE INDEX idx_transactions_account ON transactions(account_id);
CREATE INDEX idx_transactions_category ON transactions(category_id);
CREATE INDEX idx_transactions_client ON transactions(client_id);
CREATE INDEX idx_transactions_date ON transactions(transaction_date);
