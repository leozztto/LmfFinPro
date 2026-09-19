-- Perf: mesma justificativa da V6 — category_rules e import_batches são sempre filtrados por
-- user_id (motor de categorização e listagem de importações), sem índice automático em FK.
CREATE INDEX idx_category_rules_user ON category_rules(user_id);
CREATE INDEX idx_import_batches_user ON import_batches(user_id);

-- A tela de preview/revisão busca as transações de um lote de importação por import_batch_id.
CREATE INDEX idx_transactions_import_batch ON transactions(import_batch_id);
