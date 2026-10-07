package com.lmf.finpro.infrastructure.persistence.specification;

import com.lmf.finpro.domain.model.TransactionSearchCriteria;
import com.lmf.finpro.infrastructure.persistence.entity.AccountJpaEntity;
import com.lmf.finpro.infrastructure.persistence.entity.HouseholdMemberJpaEntity;
import com.lmf.finpro.infrastructure.persistence.entity.TransactionAttachmentJpaEntity;
import com.lmf.finpro.infrastructure.persistence.entity.TransactionJpaEntity;
import com.lmf.finpro.infrastructure.persistence.entity.TransactionTagJpaEntity;
import com.lmf.finpro.infrastructure.persistence.entity.TransferJpaEntity;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
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

            // Sempre presente: ninguém enxerga transação de outro espaço. A exceção, só quando a
            // listagem pede, são as pernas de transferências feitas com contas daqui que estão num
            // espaço de que o mesmo usuário também participa.
            Predicate ownHousehold = cb.equal(account.get("householdId"), criteria.householdId());
            Long linkedUserId = criteria.includeLinkedTransferLegsOfUserId();
            predicates.add(
                    linkedUserId == null
                            ? ownHousehold
                            : cb.or(
                                    ownHousehold,
                                    linkedTransferLeg(
                                            root,
                                            query,
                                            cb,
                                            account,
                                            criteria.householdId(),
                                            linkedUserId)));

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
                predicates.add(
                        cb.greaterThanOrEqualTo(root.get("baseAmount"), criteria.minAmount()));
            }
            if (criteria.maxAmount() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("baseAmount"), criteria.maxAmount()));
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
            if (!criteria.tagIds().isEmpty()) {
                // "Qualquer uma das tags": EXISTS no vínculo, sem duplicar a transação que tiver
                // mais de uma das tags escolhidas (o que um JOIN faria).
                Subquery<Long> tagged = query.subquery(Long.class);
                Root<TransactionTagJpaEntity> link = tagged.from(TransactionTagJpaEntity.class);
                tagged.select(link.get("transactionId"))
                        .where(
                                cb.equal(link.get("transactionId"), root.get("id")),
                                link.get("tagId").in(criteria.tagIds()));
                predicates.add(cb.exists(tagged));
            }
            if (criteria.hasAttachment() != null) {
                Subquery<Long> attached = query.subquery(Long.class);
                Root<TransactionAttachmentJpaEntity> attachment =
                        attached.from(TransactionAttachmentJpaEntity.class);
                attached.select(attachment.get("id"))
                        .where(cb.equal(attachment.get("transactionId"), root.get("id")));
                predicates.add(
                        criteria.hasAttachment()
                                ? cb.exists(attached)
                                : cb.not(cb.exists(attached)));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    /**
     * Perna de transferência numa conta de outro espaço, quando a transferência tem uma ponta numa
     * conta deste espaço e o usuário participa do espaço da conta da perna. Ex.: do espaço pessoal,
     * a entrada na conta conjunta (do grupo) de uma transferência saída de uma conta pessoal.
     */
    private static Predicate linkedTransferLeg(
            Root<TransactionJpaEntity> root,
            CriteriaQuery<?> query,
            CriteriaBuilder cb,
            Join<TransactionJpaEntity, AccountJpaEntity> account,
            Long householdId,
            Long userId) {
        Subquery<Long> transferWithAccountHere = query.subquery(Long.class);
        Root<TransferJpaEntity> transfer = transferWithAccountHere.from(TransferJpaEntity.class);
        Join<TransferJpaEntity, AccountJpaEntity> from = transfer.join("fromAccount");
        Join<TransferJpaEntity, AccountJpaEntity> to = transfer.join("toAccount");
        transferWithAccountHere
                .select(transfer.get("id"))
                .where(
                        cb.equal(transfer.get("id"), root.get("transferId")),
                        cb.or(
                                cb.equal(from.get("householdId"), householdId),
                                cb.equal(to.get("householdId"), householdId)));

        Subquery<Long> userHouseholds = query.subquery(Long.class);
        Root<HouseholdMemberJpaEntity> member = userHouseholds.from(HouseholdMemberJpaEntity.class);
        userHouseholds
                .select(member.get("householdId"))
                .where(cb.equal(member.get("userId"), userId));

        return cb.and(
                cb.isNotNull(root.get("transferId")),
                cb.exists(transferWithAccountHere),
                account.get("householdId").in(userHouseholds));
    }

    /** "%" e "_" digitados pelo usuário são texto, não curingas do LIKE. */
    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
