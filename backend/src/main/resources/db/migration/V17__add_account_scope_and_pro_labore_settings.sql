-- Separação PF/PJ: cada conta é pessoal (PERSONAL) ou da empresa (BUSINESS). As contas que já
-- existem ficam como pessoais — o cálculo de pró-labore só considera o que o usuário marcar como PJ.
ALTER TABLE accounts ADD COLUMN scope VARCHAR(20) NOT NULL DEFAULT 'PERSONAL';

-- Configuração do cálculo de pró-labore. Sem linha aqui vale o padrão (1 mês de colchão de caixa,
-- ver ProLaboreSettings.defaults).
CREATE TABLE pro_labore_settings (
    user_id             BIGINT   PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    cash_cushion_months INTEGER  NOT NULL DEFAULT 1
);
