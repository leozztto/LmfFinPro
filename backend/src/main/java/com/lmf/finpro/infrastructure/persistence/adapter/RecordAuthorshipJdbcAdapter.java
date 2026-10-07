package com.lmf.finpro.infrastructure.persistence.adapter;

import com.lmf.finpro.domain.port.out.RecordAuthorshipPort;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/** A coluna {@code created_by} não é mapeada nas entidades JPA: é lida e gravada só por aqui. */
@Component
@RequiredArgsConstructor
public class RecordAuthorshipJdbcAdapter implements RecordAuthorshipPort {

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public void recordTransactionAuthors(Collection<Long> transactionIds, Long userId) {
        if (transactionIds.isEmpty() || userId == null) {
            return;
        }
        jdbc.update(
                "UPDATE transactions SET created_by = :user WHERE id IN (:ids)",
                new MapSqlParameterSource()
                        .addValue("user", userId)
                        .addValue("ids", transactionIds));
    }

    @Override
    public void recordTransferAuthor(Long transferId, Long userId) {
        if (userId == null) {
            return;
        }
        jdbc.update(
                "UPDATE transfers SET created_by = :user WHERE id = :id",
                new MapSqlParameterSource().addValue("user", userId).addValue("id", transferId));
    }

    @Override
    public Optional<Long> findTransactionAuthor(Long transactionId) {
        return Optional.ofNullable(
                findTransactionAuthors(List.of(transactionId)).get(transactionId));
    }

    @Override
    public Optional<Long> findTransferAuthor(Long transferId) {
        return Optional.ofNullable(findTransferAuthors(List.of(transferId)).get(transferId));
    }

    @Override
    public Map<Long, Long> findTransactionAuthors(Collection<Long> transactionIds) {
        return authors("transactions", transactionIds);
    }

    @Override
    public Map<Long, Long> findTransferAuthors(Collection<Long> transferIds) {
        return authors("transfers", transferIds);
    }

    private Map<Long, Long> authors(String table, Collection<Long> ids) {
        Map<Long, Long> authors = new HashMap<>();
        if (ids.isEmpty()) {
            return authors;
        }
        jdbc.query(
                "SELECT id, created_by FROM "
                        + table
                        + " WHERE id IN (:ids) AND created_by IS NOT NULL",
                new MapSqlParameterSource("ids", ids),
                rs -> {
                    authors.put(rs.getLong("id"), rs.getLong("created_by"));
                });
        return authors;
    }
}
