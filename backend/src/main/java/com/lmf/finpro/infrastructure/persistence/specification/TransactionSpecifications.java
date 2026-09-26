package com.lmf.finpro.infrastructure.persistence.specification;

import com.lmf.finpro.domain.model.TransactionSearchCriteria;
import com.lmf.finpro.infrastructure.persistence.entity.AccountJpaEntity;
import com.lmf.finpro.infrastructure.persistence.entity.TransactionJpaEntity;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

/**
 * Monta a consulta de transações a partir de {@link TransactionSearchCriteria}: cada filtro vira um
 * predicado <b>só quando foi informado</b> — filtro nulo não entra no SQL (nada de {@code :param IS
 * NULL OR ...}).
 */
public final class TransactionSpecifications {

    private static final char LIKE_ESCAPE = '\\';

    private TransactionSpecifications() {}

    public static Specification<TransactionJpaEntity> matching(TransactionSearchCriteria criteria) {
        return (root, query, cb) -> {
            Join<TransactionJpaEntity, AccountJpaEntity> account = root.join("account");
            List<Predicate> predicates = new ArrayList<>();

            // Sempre presente: ninguém enxerga transação de outro usuário.
            predicates.add(cb.equal(account.get("user").get("id"), criteria.userId()));

            if (criteria.excludeTransfers()) {
                predicates.add(cb.isNull(root.get("transferId")));
            }
            if (criteria.type() != null) {
                predicates.add(cb.equal(root.get("type"), criteria.type()));
            }
            if (criteria.startDate() != null) {
                predicates.add(
                        cb.greaterThanOrEqualTo(root.get("transactionDate"), criteria.startDate()));
            }
            if (criteria.endDate() != null) {
                predicates.add(
                        cb.lessThanOrEqualTo(root.get("transactionDate"), criteria.endDate()));
            }
            if (criteria.accountId() != null) {
                predicates.add(cb.equal(account.get("id"), criteria.accountId()));
            }
            if (criteria.accountScope() != null) {
                predicates.add(cb.equal(account.get("scope"), criteria.accountScope()));
            }
            if (criteria.categoryId() != null) {
                predicates.add(cb.equal(root.get("category").get("id"), criteria.categoryId()));
            }
            if (criteria.clientId() != null) {
                predicates.add(cb.equal(root.get("client").get("id"), criteria.clientId()));
            }
            if (criteria.status() != null) {
                predicates.add(cb.equal(root.get("status"), criteria.status()));
            }
            if (criteria.minAmount() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("amount"), criteria.minAmount()));
            }
            if (criteria.maxAmount() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("amount"), criteria.maxAmount()));
            }
            if (criteria.description() != null) {
                predicates.add(
                        cb.like(
                                cb.lower(root.get("description")),
                                "%"
                                        + escapeLike(
                                                criteria.description().toLowerCase(Locale.ROOT))
                                        + "%",
                                LIKE_ESCAPE));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    /** "%" e "_" digitados pelo usuário são texto, não curingas do LIKE. */
    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
