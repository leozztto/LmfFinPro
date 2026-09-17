-- CPF/CNPJ: usuário pode se cadastrar como pessoa física (CPF) ou jurídica (CNPJ),
-- dependendo do regime tributário. A coluna passa a comportar os dois formatos.

ALTER TABLE users RENAME COLUMN cpf TO document_number;
ALTER TABLE users ALTER COLUMN document_number TYPE VARCHAR(14);
ALTER TABLE users RENAME CONSTRAINT uq_users_cpf TO uq_users_document_number;

ALTER TABLE users ADD COLUMN document_type VARCHAR(4) NOT NULL DEFAULT 'CPF';
