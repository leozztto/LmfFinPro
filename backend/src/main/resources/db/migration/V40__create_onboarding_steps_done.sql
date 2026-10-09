-- Guia de primeiros passos: cada passo visto fica gravado por usuário, com a data, para retomar do último
-- ponto. Quais passos existem é regra do backend (enum StepId), por isso a tabela não restringe o valor.
CREATE TABLE onboarding_steps_done (
    user_id      BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    step         VARCHAR(30) NOT NULL,
    completed_at TIMESTAMP   NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, step)
);
