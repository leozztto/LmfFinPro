-- Inscrições de Web Push (um registro por navegador/aparelho do usuário). O endpoint é a URL do
-- serviço de push do navegador e identifica a inscrição; p256dh e auth são as chaves públicas
-- usadas para cifrar a mensagem. Inscrições expiradas (410/404) são removidas no envio.
CREATE TABLE push_subscriptions (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    endpoint    VARCHAR(1000) NOT NULL UNIQUE,
    p256dh      VARCHAR(200)  NOT NULL,
    auth        VARCHAR(100)  NOT NULL,
    created_at  TIMESTAMP     NOT NULL DEFAULT now()
);

CREATE INDEX idx_push_subscriptions_user ON push_subscriptions(user_id);
