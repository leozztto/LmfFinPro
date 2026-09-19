-- Identifica se o usuário atende este cliente como PJ (via empresa própria) ou como autônomo/freelancer
-- (pessoa física). Opcional: nem todo mundo sabe/precisa classificar isso no momento do cadastro.
ALTER TABLE clients ADD COLUMN work_type VARCHAR(10);
