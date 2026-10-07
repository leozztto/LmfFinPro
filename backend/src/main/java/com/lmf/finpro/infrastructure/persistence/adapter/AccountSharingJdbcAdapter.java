package com.lmf.finpro.infrastructure.persistence.adapter;

import com.lmf.finpro.domain.model.BlockingLink;
import com.lmf.finpro.domain.port.out.AccountSharingPort;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * SQL em lote: mover uma conta de grupo troca dezenas ou milhares de linhas (transações,
 * anexos...), e carregar cada uma como entidade só para trocar uma coluna seria desperdício. Roda
 * na transação do caso de uso (o JdbcTemplate usa a mesma conexão do JPA).
 */
@Component
@RequiredArgsConstructor
public class AccountSharingJdbcAdapter implements AccountSharingPort {

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public List<BlockingLink> findBlockingLinks(Set<Long> accountIds) {
        MapSqlParameterSource params = new MapSqlParameterSource("ids", accountIds);
        return jdbc.query(
                """
SELECT DISTINCT 'SAVINGS_GOAL' AS kind, a.name AS account_name, f.name AS other_name
FROM savings_goals g
JOIN accounts a ON a.id = g.account_id
JOIN accounts f ON f.id = g.funding_account_id
WHERE g.account_id IN (:ids) AND g.funding_account_id NOT IN (:ids)
UNION
SELECT DISTINCT 'SAVINGS_GOAL', f.name, a.name
FROM savings_goals g
JOIN accounts a ON a.id = g.account_id
JOIN accounts f ON f.id = g.funding_account_id
WHERE g.funding_account_id IN (:ids) AND g.account_id NOT IN (:ids)
""",
                params,
                (rs, row) ->
                        new BlockingLink(
                                BlockingLink.Type.valueOf(rs.getString("kind")),
                                rs.getString("account_name"),
                                rs.getString("other_name")));
    }

    @Override
    public Set<Long> findCategoryIdsUsedBy(Set<Long> accountIds) {
        return queryIds(
                """
                SELECT category_id FROM transactions
                WHERE account_id IN (:ids) AND category_id IS NOT NULL
                UNION
                SELECT category_id FROM recurring_transactions
                WHERE account_id IN (:ids) AND category_id IS NOT NULL
                """,
                accountIds);
    }

    @Override
    public Set<Long> findClientIdsUsedBy(Set<Long> accountIds) {
        return queryIds(
                """
                SELECT client_id FROM transactions
                WHERE account_id IN (:ids) AND client_id IS NOT NULL
                UNION
                SELECT client_id FROM recurring_transactions
                WHERE account_id IN (:ids) AND client_id IS NOT NULL
                """,
                accountIds);
    }

    @Override
    public Set<Long> findTagIdsUsedBy(Set<Long> accountIds) {
        return queryIds(
                """
                SELECT tt.tag_id FROM transaction_tags tt
                JOIN transactions t ON t.id = tt.transaction_id
                WHERE t.account_id IN (:ids)
                UNION
                SELECT rt.tag_id FROM recurring_transaction_tags rt
                JOIN recurring_transactions r ON r.id = rt.recurring_transaction_id
                WHERE r.account_id IN (:ids)
                """,
                accountIds);
    }

    @Override
    public void remapCategories(Set<Long> accountIds, Map<Long, Long> oldToNew) {
        oldToNew.forEach(
                (oldId, newId) -> {
                    update(
                            "UPDATE transactions SET category_id = :new"
                                    + " WHERE account_id IN (:ids) AND category_id = :old",
                            accountIds,
                            oldId,
                            newId);
                    update(
                            "UPDATE recurring_transactions SET category_id = :new"
                                    + " WHERE account_id IN (:ids) AND category_id = :old",
                            accountIds,
                            oldId,
                            newId);
                });
    }

    @Override
    public void remapClients(Set<Long> accountIds, Map<Long, Long> oldToNew) {
        oldToNew.forEach(
                (oldId, newId) -> {
                    update(
                            "UPDATE transactions SET client_id = :new"
                                    + " WHERE account_id IN (:ids) AND client_id = :old",
                            accountIds,
                            oldId,
                            newId);
                    update(
                            "UPDATE recurring_transactions SET client_id = :new"
                                    + " WHERE account_id IN (:ids) AND client_id = :old",
                            accountIds,
                            oldId,
                            newId);
                });
    }

    @Override
    public void remapTags(Set<Long> accountIds, Map<Long, Long> oldToNew) {
        oldToNew.forEach(
                (oldId, newId) -> {
                    update(
                            "UPDATE transaction_tags SET tag_id = :new WHERE tag_id = :old"
                                    + " AND transaction_id IN"
                                    + " (SELECT id FROM transactions WHERE account_id IN (:ids))",
                            accountIds,
                            oldId,
                            newId);
                    update(
                            "UPDATE recurring_transaction_tags SET tag_id = :new"
                                    + " WHERE tag_id = :old AND recurring_transaction_id IN"
                                    + " (SELECT id FROM recurring_transactions"
                                    + " WHERE account_id IN (:ids))",
                            accountIds,
                            oldId,
                            newId);
                });
    }

    @Override
    public int countTransactions(Set<Long> accountIds) {
        Integer count =
                jdbc.queryForObject(
                        "SELECT count(*) FROM transactions WHERE account_id IN (:ids)",
                        new MapSqlParameterSource("ids", accountIds),
                        Integer.class);
        return count == null ? 0 : count;
    }

    @Override
    public void moveToHousehold(Set<Long> accountIds, Long targetHouseholdId) {
        MapSqlParameterSource params =
                new MapSqlParameterSource()
                        .addValue("ids", accountIds)
                        .addValue("target", targetHouseholdId);
        // Transferência entre uma conta movida e uma que fica: divide em duas, uma por espaço.
        splitCrossingTransfers(accountIds, targetHouseholdId);
        // As transações não têm grupo: seguem a conta. As metas só chegam aqui com as duas contas
        // no conjunto (findBlockingLinks barra o resto), e as transferências que sobram também.
        for (String sql :
                List.of(
                        "UPDATE accounts SET household_id = :target WHERE id IN (:ids)",
                        "UPDATE import_batches SET household_id = :target"
                                + " WHERE account_id IN (:ids)",
                        "UPDATE recurring_transactions SET household_id = :target"
                                + " WHERE account_id IN (:ids)",
                        "UPDATE transfers SET household_id = :target"
                                + " WHERE from_account_id IN (:ids) AND to_account_id IN (:ids)",
                        "UPDATE savings_goals SET household_id = :target"
                                + " WHERE account_id IN (:ids)",
                        "UPDATE transaction_attachments SET household_id = :target"
                                + " WHERE transaction_id IN"
                                + " (SELECT id FROM transactions WHERE account_id IN (:ids))")) {
            jdbc.update(sql, params);
        }
    }

    /**
     * Uma transferência entre uma conta que vai para o grupo e outra que fica não pode pertencer a
     * dois grupos. Em vez de barrar (o que obrigaria a expor a outra conta), ela é dividida: cada
     * espaço fica com a sua perna, ligada a uma transferência própria, e cada um enxerga só o que é
     * seu. O nome da conta do outro lado continua aparecendo (a tela de transferências mostra de
     * onde veio o dinheiro, e a descrição automática o cita), mas só o nome: nenhum outro dado
     * dessa conta fica visível.
     *
     * <p>Se a outra conta já foi compartilhada antes, a divisão já existe: a perna volta para a
     * transferência do grupo e as duas metades se juntam de novo.
     */
    private void splitCrossingTransfers(Set<Long> accountIds, Long targetHouseholdId) {
        List<CrossingTransfer> crossing =
                jdbc.query(
                        """
                        SELECT t.id, t.from_account_id, t.to_account_id, t.amount, t.transfer_date,
                               t.description
                        FROM transfers t
                        WHERE (t.from_account_id IN (:ids)) <> (t.to_account_id IN (:ids))
                          -- Só as do espaço de onde as contas saem: a metade que já está em um
                          -- grupo (de uma compartilhação anterior) não é cruzada, é o destino.
                          AND t.household_id = (SELECT a.household_id FROM accounts a
                                                WHERE a.id IN (:ids) LIMIT 1)
                        """,
                        new MapSqlParameterSource("ids", accountIds),
                        (rs, row) ->
                                new CrossingTransfer(
                                        rs.getLong("id"),
                                        rs.getLong("from_account_id"),
                                        rs.getLong("to_account_id"),
                                        rs.getBigDecimal("amount"),
                                        rs.getDate("transfer_date").toLocalDate(),
                                        rs.getString("description")));

        for (CrossingTransfer transfer : crossing) {
            Long sibling = findSiblingInTarget(transfer, targetHouseholdId);
            Long targetTransferId =
                    sibling != null ? sibling : copyTransfer(transfer, targetHouseholdId);

            // A perna da conta que se move passa para a transferência do grupo.
            jdbc.update(
                    "UPDATE transactions SET transfer_id = :target_transfer"
                            + " WHERE transfer_id = :source_transfer AND account_id IN (:ids)",
                    new MapSqlParameterSource()
                            .addValue("target_transfer", targetTransferId)
                            .addValue("source_transfer", transfer.id())
                            .addValue("ids", accountIds));

            if (sibling != null) {
                // As duas metades se juntaram: a transferência de origem ficou sem pernas.
                jdbc.update(
                        "DELETE FROM transfers WHERE id = :id AND NOT EXISTS"
                                + " (SELECT 1 FROM transactions WHERE transfer_id = :id)",
                        new MapSqlParameterSource("id", transfer.id()));
            }
        }
    }

    /** A metade da mesma transferência que já está no grupo, de uma compartilhação anterior. */
    private Long findSiblingInTarget(CrossingTransfer transfer, Long targetHouseholdId) {
        List<Long> found =
                jdbc.queryForList(
                        """
                        SELECT id FROM transfers
                        WHERE household_id = :target AND id <> :id
                          AND from_account_id = :from AND to_account_id = :to
                          AND amount = :amount AND transfer_date = :date
                          AND COALESCE(description, '') = COALESCE(:description, '')
                        ORDER BY id LIMIT 1
                        """,
                        new MapSqlParameterSource()
                                .addValue("target", targetHouseholdId)
                                .addValue("id", transfer.id())
                                .addValue("from", transfer.fromAccountId())
                                .addValue("to", transfer.toAccountId())
                                .addValue("amount", transfer.amount())
                                .addValue("date", java.sql.Date.valueOf(transfer.date()))
                                .addValue("description", transfer.description()),
                        Long.class);
        return found.isEmpty() ? null : found.get(0);
    }

    private Long copyTransfer(CrossingTransfer transfer, Long targetHouseholdId) {
        return jdbc.queryForObject(
                """
                INSERT INTO transfers (household_id, from_account_id, to_account_id, amount,
                                       received_amount, transfer_date, description, created_at,
                                       created_by)
                SELECT :target, from_account_id, to_account_id, amount, received_amount,
                       transfer_date, description, created_at, created_by
                FROM transfers WHERE id = :id
                RETURNING id
                """,
                new MapSqlParameterSource()
                        .addValue("target", targetHouseholdId)
                        .addValue("id", transfer.id()),
                Long.class);
    }

    private record CrossingTransfer(
            Long id,
            Long fromAccountId,
            Long toAccountId,
            java.math.BigDecimal amount,
            java.time.LocalDate date,
            String description) {}

    private Set<Long> queryIds(String sql, Set<Long> accountIds) {
        return new LinkedHashSet<>(
                jdbc.queryForList(sql, new MapSqlParameterSource("ids", accountIds), Long.class));
    }

    private void update(String sql, Set<Long> accountIds, Long oldId, Long newId) {
        jdbc.update(
                sql,
                new MapSqlParameterSource()
                        .addValue("ids", accountIds)
                        .addValue("old", oldId)
                        .addValue("new", newId));
    }
}
