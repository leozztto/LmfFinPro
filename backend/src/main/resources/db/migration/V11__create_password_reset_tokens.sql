-- Tokens de redefinição de senha ("esqueci minha senha"). Só o hash SHA-256 do token é guardado:
-- quem tiver acesso ao banco não consegue usar os tokens para trocar a senha de ninguém.
-- Cada token é de uso único (used_at) e expira em expires_at.

CREATE TABLE password_reset_tokens (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash   VARCHAR(64)   NOT NULL UNIQUE,
    expires_at   TIMESTAMP     NOT NULL,
    used_at      TIMESTAMP,
    created_at   TIMESTAMP     NOT NULL DEFAULT now()
);

CREATE INDEX idx_password_reset_tokens_user ON password_reset_tokens(user_id);
