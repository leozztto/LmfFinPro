-- Dono da conta: quem a criou ou a trouxe para um grupo compartilhado. Só ele pode descompartilhá-la
-- (devolvê-la ao próprio espaço pessoal). Nulo = desconhecido (contas que já estavam em grupos
-- antes desta migration): nesse caso quem pode descompartilhar é o dono do grupo.
-- ON DELETE SET NULL: se o usuário for excluído, a conta fica no grupo, sem dono.
ALTER TABLE accounts ADD COLUMN owner_user_id BIGINT REFERENCES users(id) ON DELETE SET NULL;

-- Contas que já estão no espaço pessoal: o dono é a única pessoa dele.
UPDATE accounts a
SET owner_user_id = m.user_id
FROM households h
JOIN household_members m ON m.household_id = h.id
WHERE h.id = a.household_id AND h.type = 'PERSONAL';
