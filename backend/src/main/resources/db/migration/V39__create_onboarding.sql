-- Estado do onboarding por usuário: se dispensou o guia de primeiros passos e se aceita os e-mails
-- de ativação. O progresso em si (conta, lançamentos, orçamento) é calculado dos dados, não gravado.
CREATE TABLE onboarding_state
(
    user_id                    BIGINT PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    dismissed_at               TIMESTAMP NULL,
    activation_emails_enabled  BOOLEAN   NOT NULL DEFAULT TRUE
);

-- Um registro por e-mail de ativação já enviado: garante que cada um sai uma única vez.
CREATE TABLE activation_emails_sent
(
    user_id  BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    kind     VARCHAR(40) NOT NULL,
    sent_at  TIMESTAMP   NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, kind)
);
