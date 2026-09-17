-- Cadastro mais detalhado: endereço do usuário (útil futuramente para emissão de
-- recibos/cobranças por cliente e para o regime tributário informado).

ALTER TABLE users ADD COLUMN address_zip_code VARCHAR(8);
ALTER TABLE users ADD COLUMN address_street VARCHAR(150);
ALTER TABLE users ADD COLUMN address_number VARCHAR(20);
ALTER TABLE users ADD COLUMN address_complement VARCHAR(100);
ALTER TABLE users ADD COLUMN address_neighborhood VARCHAR(100);
ALTER TABLE users ADD COLUMN address_city VARCHAR(100);
ALTER TABLE users ADD COLUMN address_state VARCHAR(2);
