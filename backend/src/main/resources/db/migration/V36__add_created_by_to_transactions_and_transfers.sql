-- Autoria: quem criou cada lançamento e cada transferência. Numa conta compartilhada (grupo), só
-- quem criou o registro pode excluí-lo. Nulo = autor desconhecido (criado pelo sistema, como as
-- ocorrências de lançamentos recorrentes e os aportes automáticos de metas, ou antes de a autoria
-- existir): qualquer membro do grupo pode excluir.
-- ON DELETE SET NULL: se o usuário for excluído, o registro fica no grupo, sem autor.
ALTER TABLE transactions ADD COLUMN created_by BIGINT REFERENCES users(id) ON DELETE SET NULL;
ALTER TABLE transfers ADD COLUMN created_by BIGINT REFERENCES users(id) ON DELETE SET NULL;

-- Registros que já existem no espaço pessoal: o autor só pode ser o dono (o espaço pessoal tem um
-- membro só). Os que já estão em grupos compartilhados ficam sem autor: não há como saber.
UPDATE transactions t
SET created_by = m.user_id
FROM accounts a
JOIN households h ON h.id = a.household_id AND h.type = 'PERSONAL'
JOIN household_members m ON m.household_id = h.id
WHERE t.account_id = a.id;

UPDATE transfers tr
SET created_by = m.user_id
FROM households h
JOIN household_members m ON m.household_id = h.id
WHERE h.id = tr.household_id AND h.type = 'PERSONAL';
