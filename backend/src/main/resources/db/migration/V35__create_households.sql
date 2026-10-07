-- Contas compartilhadas (casal/família): o grupo (household) passa a ser o dono dos dados financeiros.
-- Todo usuário tem um grupo PERSONAL (só dele, criado no cadastro) e pode participar de grupos
-- SHARED (casal/família) com papel OWNER ou MEMBER; os dados de cada grupo ficam separados e a
-- tela alterna entre eles.
CREATE TABLE households (
    id         BIGSERIAL    PRIMARY KEY,
    name       VARCHAR(150) NOT NULL,
    type       VARCHAR(20)  NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT ck_households_type CHECK (type IN ('PERSONAL', 'SHARED'))
);

CREATE TABLE household_members (
    id           BIGSERIAL   PRIMARY KEY,
    household_id BIGINT      NOT NULL REFERENCES households(id) ON DELETE CASCADE,
    user_id      BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role         VARCHAR(20) NOT NULL,
    joined_at    TIMESTAMP   NOT NULL DEFAULT now(),
    CONSTRAINT uq_household_members UNIQUE (household_id, user_id),
    CONSTRAINT ck_household_members_role CHECK (role IN ('OWNER', 'MEMBER'))
);

CREATE INDEX idx_household_members_household ON household_members(household_id);
CREATE INDEX idx_household_members_user ON household_members(user_id);

-- Convites para entrar em um grupo. Só o hash do token é guardado (como em refresh_tokens); o token
-- em claro vai apenas no link enviado por e-mail. Uso único (accepted_at) e com expiração.
CREATE TABLE household_invites (
    id           BIGSERIAL    PRIMARY KEY,
    household_id BIGINT       NOT NULL REFERENCES households(id) ON DELETE CASCADE,
    email        VARCHAR(150) NOT NULL,
    token_hash   VARCHAR(64)  NOT NULL,
    role         VARCHAR(20)  NOT NULL DEFAULT 'MEMBER',
    created_by   BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expires_at   TIMESTAMP    NOT NULL,
    accepted_at  TIMESTAMP,
    created_at   TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT uq_household_invites_token UNIQUE (token_hash),
    CONSTRAINT ck_household_invites_role CHECK (role IN ('OWNER', 'MEMBER'))
);

CREATE INDEX idx_household_invites_household ON household_invites(household_id);
CREATE INDEX idx_household_invites_email ON household_invites(lower(email));

-- Os dados financeiros passam a pertencer ao grupo: household_id substitui user_id, que sai das
-- tabelas (a autoria de cada lançamento volta depois, como created_by). Os índices e a unicidade que
-- usavam user_id caem junto com a coluna.
--
-- transactions não tem user_id: pertence ao grupo pela conta (accounts.household_id).
-- sent_alerts e notification_preferences ficam por usuário: o alerta é por destinatário.
-- categories e category_rules com household_id nulo são globais do sistema (seed da V25).

ALTER TABLE accounts DROP COLUMN user_id,
    ADD COLUMN household_id BIGINT NOT NULL REFERENCES households(id) ON DELETE CASCADE;
ALTER TABLE clients DROP COLUMN user_id,
    ADD COLUMN household_id BIGINT NOT NULL REFERENCES households(id) ON DELETE CASCADE;
ALTER TABLE import_batches DROP COLUMN user_id,
    ADD COLUMN household_id BIGINT NOT NULL REFERENCES households(id) ON DELETE CASCADE;
ALTER TABLE tax_estimates DROP COLUMN user_id,
    ADD COLUMN household_id BIGINT NOT NULL REFERENCES households(id) ON DELETE CASCADE;
ALTER TABLE budgets DROP COLUMN user_id,
    ADD COLUMN household_id BIGINT NOT NULL REFERENCES households(id) ON DELETE CASCADE;
ALTER TABLE recurring_budgets DROP COLUMN user_id,
    ADD COLUMN household_id BIGINT NOT NULL REFERENCES households(id) ON DELETE CASCADE;
ALTER TABLE transfers DROP COLUMN user_id,
    ADD COLUMN household_id BIGINT NOT NULL REFERENCES households(id) ON DELETE CASCADE;
ALTER TABLE recurring_transactions DROP COLUMN user_id,
    ADD COLUMN household_id BIGINT NOT NULL REFERENCES households(id) ON DELETE CASCADE;
ALTER TABLE savings_goals DROP COLUMN user_id,
    ADD COLUMN household_id BIGINT NOT NULL REFERENCES households(id) ON DELETE CASCADE;
ALTER TABLE transaction_attachments DROP COLUMN user_id,
    ADD COLUMN household_id BIGINT NOT NULL REFERENCES households(id) ON DELETE CASCADE;
ALTER TABLE debts DROP COLUMN user_id,
    ADD COLUMN household_id BIGINT NOT NULL REFERENCES households(id) ON DELETE CASCADE;
ALTER TABLE tags DROP COLUMN user_id,
    ADD COLUMN household_id BIGINT NOT NULL REFERENCES households(id) ON DELETE CASCADE;

-- Globais do sistema (seed da V25) ficam com household_id nulo.
ALTER TABLE categories DROP COLUMN user_id,
    ADD COLUMN household_id BIGINT REFERENCES households(id) ON DELETE CASCADE;
ALTER TABLE category_rules DROP COLUMN user_id,
    ADD COLUMN household_id BIGINT REFERENCES households(id) ON DELETE CASCADE;

CREATE INDEX idx_accounts_household ON accounts(household_id);
CREATE INDEX idx_categories_household ON categories(household_id);
CREATE INDEX idx_category_rules_household ON category_rules(household_id);
CREATE INDEX idx_clients_household ON clients(household_id);
CREATE INDEX idx_import_batches_household ON import_batches(household_id);
CREATE INDEX idx_tax_estimates_household ON tax_estimates(household_id);
CREATE INDEX idx_budgets_household ON budgets(household_id);
CREATE INDEX idx_recurring_budgets_household ON recurring_budgets(household_id);
CREATE INDEX idx_transfers_household ON transfers(household_id);
CREATE INDEX idx_recurring_transactions_household ON recurring_transactions(household_id);
CREATE INDEX idx_savings_goals_household ON savings_goals(household_id);
CREATE INDEX idx_transaction_attachments_household ON transaction_attachments(household_id);
CREATE INDEX idx_debts_household ON debts(household_id);

-- Etiquetas: o nome é único por grupo, não por usuário.
CREATE UNIQUE INDEX uq_tags_household_name ON tags(household_id, name);

-- Configuração do pró-labore: uma por grupo (a chave deixa de ser o usuário).
ALTER TABLE pro_labore_settings DROP COLUMN user_id,
    ADD COLUMN household_id BIGINT PRIMARY KEY REFERENCES households(id) ON DELETE CASCADE;
