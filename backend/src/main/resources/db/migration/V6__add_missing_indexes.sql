-- Perf: accounts/categories/clients são sempre filtrados por user_id (toda listagem do usuário
-- passa por aqui), mas o Postgres não cria índice automático em coluna de FK (só em PK/unique) —
-- sem isso, cada consulta faz sequential scan na tabela inteira.
CREATE INDEX idx_accounts_user ON accounts(user_id);
CREATE INDEX idx_categories_user ON categories(user_id);
CREATE INDEX idx_clients_user ON clients(user_id);

-- Perf: substitui o índice simples em account_id por um composto (account_id, type). Continua
-- atendendo consultas por account_id sozinho (leftmost prefix) e passa a atender também a soma
-- de saldo por tipo (SUM(amount) WHERE account_id = ? AND type = ?) usada na validação de
-- transferências, sem precisar ler as linhas da tabela.
DROP INDEX idx_transactions_account;
CREATE INDEX idx_transactions_account_type ON transactions(account_id, type);
