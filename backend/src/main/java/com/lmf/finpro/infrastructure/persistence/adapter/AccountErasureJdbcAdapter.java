package com.lmf.finpro.infrastructure.persistence.adapter;

import com.lmf.finpro.domain.port.out.AccountErasurePort;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Todas as tabelas de dados fazem {@code ON DELETE CASCADE} a partir de {@code households}, então
 * um único DELETE leva contas, lançamentos, anexos (registros), metas etc. As FKs do usuário que
 * apontam para dados que ficam em grupos compartilhados (autoria, dono da conta) são {@code SET
 * NULL}.
 */
@Component
@RequiredArgsConstructor
public class AccountErasureJdbcAdapter implements AccountErasurePort {

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public List<String> findAttachmentKeys(Collection<Long> householdIds) {
        if (householdIds.isEmpty()) {
            return List.of();
        }
        return jdbc.queryForList(
                "SELECT storage_key FROM transaction_attachments WHERE household_id IN (:ids)",
                new MapSqlParameterSource("ids", householdIds),
                String.class);
    }

    @Override
    public int countAccountsBroughtBy(Long userId, Long householdId) {
        Integer count =
                jdbc.queryForObject(
                        "SELECT count(*) FROM accounts WHERE household_id = :household AND"
                                + " owner_user_id = :user",
                        new MapSqlParameterSource()
                                .addValue("household", householdId)
                                .addValue("user", userId),
                        Integer.class);
        return count == null ? 0 : count;
    }

    @Override
    public void deleteHouseholds(Collection<Long> householdIds) {
        if (householdIds.isEmpty()) {
            return;
        }
        jdbc.update(
                "DELETE FROM households WHERE id IN (:ids)",
                new MapSqlParameterSource("ids", householdIds));
    }

    @Override
    public void deleteInvitesAddressedTo(String email) {
        jdbc.update(
                "DELETE FROM household_invites WHERE lower(email) = lower(:email)",
                new MapSqlParameterSource("email", email));
    }

    @Override
    public void deleteUser(Long userId) {
        jdbc.update("DELETE FROM users WHERE id = :id", new MapSqlParameterSource("id", userId));
    }

    @Override
    public void logDeletion(Long userId) {
        jdbc.update(
                "INSERT INTO account_deletion_log (user_id) VALUES (:id)",
                new MapSqlParameterSource("id", userId));
    }
}
