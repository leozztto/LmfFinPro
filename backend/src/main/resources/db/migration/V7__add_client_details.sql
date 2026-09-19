-- Cadastro de cliente/projeto mais completo: até aqui a tabela só tinha nome e status ativo.
-- Os novos campos são todos opcionais para não quebrar o fluxo simples de "só cadastrar o nome".

ALTER TABLE clients ADD COLUMN email VARCHAR(150);
ALTER TABLE clients ADD COLUMN phone VARCHAR(11);
ALTER TABLE clients ADD COLUMN document_type VARCHAR(4);
ALTER TABLE clients ADD COLUMN document_number VARCHAR(14);
ALTER TABLE clients ADD COLUMN notes VARCHAR(1000);
ALTER TABLE clients ADD COLUMN color VARCHAR(20);
