-- Hora da transação (opcional): vem só de importação de extrato (CSV com coluna de hora, ou OFX
-- com DTPOSTED trazendo horário), usada pra detectar duplicidade ao reimportar um período que se
-- sobrepõe a uma importação anterior. transactionDate continua sem mudança nenhuma.
ALTER TABLE transactions ADD COLUMN transaction_time TIME;

-- Quantas linhas de uma importação foram puladas por já existir uma transação igual (mesma data,
-- hora, descrição, valor e tipo) na conta — não dá pra derivar isso das transações salvas, já que
-- uma linha pulada nunca vira uma Transaction.
ALTER TABLE import_batches ADD COLUMN duplicate_count INTEGER NOT NULL DEFAULT 0;
