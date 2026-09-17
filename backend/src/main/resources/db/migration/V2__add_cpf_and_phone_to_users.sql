-- Cadastro mais detalhado: CPF (obrigatório em novos cadastros, aplicado na camada de validação
-- para não quebrar usuários já existentes) e telefone (opcional).

ALTER TABLE users ADD COLUMN cpf VARCHAR(11);
ALTER TABLE users ADD COLUMN phone VARCHAR(11);

ALTER TABLE users ADD CONSTRAINT uq_users_cpf UNIQUE (cpf);
