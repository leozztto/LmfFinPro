-- Preferências dos alertas por e-mail. Usuário sem linha aqui recebe tudo com os valores padrão
-- (ver NotificationPreferences.defaults), então não é preciso popular a tabela para quem já existe.
CREATE TABLE notification_preferences (
    user_id          BIGINT   PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    bills_enabled    BOOLEAN  NOT NULL DEFAULT true,
    bill_days_before INTEGER  NOT NULL DEFAULT 3,
    budgets_enabled  BOOLEAN  NOT NULL DEFAULT true,
    das_enabled      BOOLEAN  NOT NULL DEFAULT true
);

-- Alertas já enviados: o resumo diário consulta esta tabela para não repetir o mesmo aviso
-- (a mesma conta, o mesmo limiar de um orçamento, o DAS da mesma competência) todo dia.
CREATE TABLE sent_alerts (
    id            BIGSERIAL    PRIMARY KEY,
    user_id       BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    alert_type    VARCHAR(20)  NOT NULL,
    reference_key VARCHAR(50)  NOT NULL,
    sent_at       TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT uk_sent_alerts_user_type_key UNIQUE (user_id, alert_type, reference_key)
);
