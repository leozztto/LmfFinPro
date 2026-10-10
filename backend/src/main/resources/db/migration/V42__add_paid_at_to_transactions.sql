-- Dia em que a pendência foi marcada como paga: base do insight "cliente que atrasa"
-- (paga depois do vencimento = atraso). NULL em quem nasceu paga ou já estava paga antes desta
-- migration, porque o dia do pagamento não foi guardado — nesses casos o atraso é desconhecido.
ALTER TABLE transactions ADD COLUMN paid_at DATE;
