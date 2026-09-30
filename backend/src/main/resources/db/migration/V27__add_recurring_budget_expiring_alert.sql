-- Alerta de orçamento recorrente perto de expirar (endMonth no mês atual ou no próximo).
ALTER TABLE notification_preferences ADD COLUMN recurring_budgets_enabled BOOLEAN NOT NULL DEFAULT true;

-- "RECURRING_BUDGET_EXPIRING" (25 caracteres) não cabe nos VARCHAR(20) anteriores.
ALTER TABLE sent_alerts ALTER COLUMN alert_type TYPE VARCHAR(30);
