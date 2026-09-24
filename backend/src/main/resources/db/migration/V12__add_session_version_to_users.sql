-- Versão das sessões do usuário, gravada em cada JWT emitido. Trocar a senha incrementa a versão,
-- e o filtro de autenticação recusa tokens com versão antiga — derruba as sessões abertas.
ALTER TABLE users ADD COLUMN session_version INTEGER NOT NULL DEFAULT 0;
