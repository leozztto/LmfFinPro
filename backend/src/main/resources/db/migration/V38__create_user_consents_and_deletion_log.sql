-- LGPD: prova do consentimento e trilha mínima das exclusões de conta.
--
-- user_consents guarda qual versão de cada documento (termos de uso, política de privacidade) o
-- usuário aceitou e quando. Não guarda IP nem user agent de propósito (minimização de dados): data
-- e versão bastam para provar o aceite. Quem já tinha conta antes desta migration não tem linhas
-- aqui, então o app pede o aceite no próximo acesso.
-- ON DELETE CASCADE: o consentimento some junto com a conta.
CREATE TABLE user_consents (
    id            BIGSERIAL    PRIMARY KEY,
    user_id       BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    document_type VARCHAR(20)  NOT NULL,
    version       VARCHAR(20)  NOT NULL,
    accepted_at   TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT uq_user_consents UNIQUE (user_id, document_type, version),
    CONSTRAINT ck_user_consents_type CHECK (document_type IN ('TERMS', 'PRIVACY'))
);

CREATE INDEX idx_user_consents_user ON user_consents(user_id);

-- Registro de que uma conta foi excluída e quando, para prestação de contas. Sem chave estrangeira
-- e sem nenhum dado pessoal: o id sozinho não identifica mais ninguém depois da exclusão.
CREATE TABLE account_deletion_log (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT    NOT NULL,
    deleted_at TIMESTAMP NOT NULL DEFAULT now()
);
