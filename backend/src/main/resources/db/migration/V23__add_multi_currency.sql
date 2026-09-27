-- Multi-moeda: a conta tem uma moeda (BRL por padrão) e a transação pode ter sido feita em outra.
-- Todo total que junta contas diferentes é consolidado em reais pelo base_amount.

ALTER TABLE accounts ADD COLUMN currency VARCHAR(3) NOT NULL DEFAULT 'BRL';

-- amount continua na moeda da conta. original_* registram a operação feita em outra moeda (ex.:
-- compra em dólar no cartão em reais). base_amount é o valor em reais, gravado ao salvar.
ALTER TABLE transactions ADD COLUMN original_currency VARCHAR(3);
ALTER TABLE transactions ADD COLUMN original_amount NUMERIC(14,2);
ALTER TABLE transactions ADD COLUMN base_amount NUMERIC(14,2);
UPDATE transactions SET base_amount = amount;
ALTER TABLE transactions ALTER COLUMN base_amount SET NOT NULL;
ALTER TABLE transactions ADD CONSTRAINT ck_transactions_original
    CHECK ((original_currency IS NULL) = (original_amount IS NULL));

-- Valor que entra na conta de destino quando as moedas das contas são diferentes.
ALTER TABLE transfers ADD COLUMN received_amount NUMERIC(14,2);

-- Cotação de fechamento (PTAX de venda do Banco Central) em reais, compartilhada entre usuários.
CREATE TABLE exchange_rates (
    id         BIGSERIAL      PRIMARY KEY,
    currency   VARCHAR(3)     NOT NULL,
    rate_date  DATE           NOT NULL,
    rate       NUMERIC(14,6)  NOT NULL,
    created_at TIMESTAMP      NOT NULL DEFAULT now(),
    CONSTRAINT uk_exchange_rates_currency_date UNIQUE (currency, rate_date)
);
