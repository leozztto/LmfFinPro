-- Insights automáticos (assinatura esquecida, despesa fora do padrão, cliente que atrasa) no
-- resumo diário de alertas. Quem já usa o app passa a recebê-los, como nos demais alertas.
-- Os tipos novos de sent_alerts (INSIGHT_*) cabem no VARCHAR(30) da V27.
ALTER TABLE notification_preferences ADD COLUMN insights_enabled BOOLEAN NOT NULL DEFAULT true;
