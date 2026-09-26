-- INSS e IRRF sobre o pró-labore. withholding_mode: AUTOMATIC (liga para Simples Nacional e Lucro
-- Presumido — MEI e autônomo não têm retenção sobre pró-labore), ENABLED ou DISABLED.
-- employer_inss_rate: INSS patronal sobre o pró-labore; nulo = automático pelo regime (20% no Lucro
-- Presumido, 0% nos demais, já que no Simples a contribuição patronal vai no DAS na maioria dos
-- anexos).
ALTER TABLE pro_labore_settings
    ADD COLUMN withholding_mode   VARCHAR(20)  NOT NULL DEFAULT 'AUTOMATIC',
    ADD COLUMN employer_inss_rate NUMERIC(6,4);
