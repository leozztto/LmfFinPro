-- Orçamento opcionalmente vinculado a um cliente/projeto: quando preenchido, o gasto do orçamento
-- considera só as despesas da categoria lançadas para aquele cliente no mês.
ALTER TABLE budgets ADD COLUMN client_id BIGINT REFERENCES clients(id) ON DELETE CASCADE;

CREATE INDEX idx_budgets_client ON budgets(client_id);
