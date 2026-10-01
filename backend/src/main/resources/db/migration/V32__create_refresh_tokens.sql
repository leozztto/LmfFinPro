-- Refresh tokens da sessão (cookie httpOnly). Só o hash SHA-256 é guardado: quem ler o banco não
-- consegue renovar a sessão de ninguém. Cada uso rotaciona o token (revoked_at) dentro da mesma
-- família (family_id); reapresentar um token já rotacionado derruba a família inteira.
-- session_version é a versão de sessão do usuário na emissão: trocar a senha a invalida.
CREATE TABLE refresh_tokens (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash       VARCHAR(64)  NOT NULL UNIQUE,
    family_id        VARCHAR(36)  NOT NULL,
    session_version  INTEGER      NOT NULL,
    expires_at       TIMESTAMP    NOT NULL,
    revoked_at       TIMESTAMP,
    created_at       TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_family ON refresh_tokens(family_id);
CREATE INDEX idx_refresh_tokens_expires ON refresh_tokens(expires_at);
