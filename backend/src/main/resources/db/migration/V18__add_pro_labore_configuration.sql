-- Pró-labore configurável: base do cálculo (receitas do mês ou saldo atual), imposto automático ou
-- manual, reserva da empresa em % da receita e pró-labore fixo opcional. Quem já tinha linha aqui
-- passa a usar os padrões novos (base receitas do mês, reserva de 10%, imposto automático).
ALTER TABLE pro_labore_settings
    ADD COLUMN calculation_base VARCHAR(20)   NOT NULL DEFAULT 'MONTH_INCOME',
    ADD COLUMN reserve_rate     NUMERIC(6,4)  NOT NULL DEFAULT 0.10,
    ADD COLUMN tax_mode         VARCHAR(20)   NOT NULL DEFAULT 'AUTOMATIC',
    ADD COLUMN manual_tax_rate  NUMERIC(6,4),
    ADD COLUMN fixed_amount     NUMERIC(14,2);
