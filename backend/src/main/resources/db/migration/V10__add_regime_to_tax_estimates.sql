-- Guarda o regime tributário usado em cada estimativa (a V1 criou a tabela sem essa coluna).
-- Sem ela, a estimativa fica sem contexto de qual alíquota/regime a originou.
ALTER TABLE tax_estimates ADD COLUMN regime VARCHAR(30) NOT NULL DEFAULT 'OUTRO';

CREATE INDEX idx_tax_estimates_user ON tax_estimates(user_id);
