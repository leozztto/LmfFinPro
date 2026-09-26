-- Situação da transação: PAID (já pago/recebido) ou PENDING (a pagar/a receber). O saldo atual
-- das contas só considera as pagas; relatórios, orçamentos e impostos seguem por competência.
ALTER TABLE transactions ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PAID';

-- Lançamentos manuais já cadastrados com data futura eram, na prática, agendamentos: passam a
-- pendentes, seguindo a mesma regra aplicada daqui para frente (data futura = pendente).
-- Importadas e transferências são sempre pagas.
UPDATE transactions
SET status = 'PENDING'
WHERE transaction_date > CURRENT_DATE
  AND origin = 'MANUAL'
  AND transfer_id IS NULL;

-- Perf: o saldo atual soma por conta, tipo e situação.
DROP INDEX idx_transactions_account_type;
CREATE INDEX idx_transactions_account_type_status ON transactions(account_id, type, status);
