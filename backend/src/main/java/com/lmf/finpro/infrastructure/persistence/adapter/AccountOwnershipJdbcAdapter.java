package com.lmf.finpro.infrastructure.persistence.adapter;

import com.lmf.finpro.domain.port.out.AccountOwnershipPort;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/** A coluna {@code owner_user_id} não é mapeada na entidade JPA: é lida e gravada só por aqui. */
@Component
@RequiredArgsConstructor
public class AccountOwnershipJdbcAdapter implements AccountOwnershipPort {

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public void recordOwner(Collection<Long> accountIds, Long userId) {
        if (accountIds.isEmpty() || userId == null) {
            return;
        }
        jdbc.update(
                "UPDATE accounts SET owner_user_id = :user WHERE id IN (:ids)",
                new MapSqlParameterSource().addValue("user", userId).addValue("ids", accountIds));
    }

    @Override
    public Optional<Long> findOwner(Long accountId) {
        return Optional.ofNullable(findOwners(List.of(accountId)).get(accountId));
    }

    @Override
    public Map<Long, Long> findOwners(Collection<Long> accountIds) {
        Map<Long, Long> owners = new HashMap<>();
        if (accountIds.isEmpty()) {
            return owners;
        }
        jdbc.query(
                "SELECT id, owner_user_id FROM accounts WHERE id IN (:ids) AND owner_user_id IS NOT"
                        + " NULL",
                new MapSqlParameterSource("ids", accountIds),
                rs -> {
                    owners.put(rs.getLong("id"), rs.getLong("owner_user_id"));
                });
        return owners;
    }
}
