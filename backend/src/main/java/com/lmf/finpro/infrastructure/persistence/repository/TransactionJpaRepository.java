package com.lmf.finpro.infrastructure.persistence.repository;

import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.infrastructure.persistence.entity.TransactionJpaEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionJpaRepository extends JpaRepository<TransactionJpaEntity, Long> {
    List<TransactionJpaEntity> findByAccountIdAndTransactionDateBetween(
            Long accountId, LocalDate start, LocalDate end);

    List<TransactionJpaEntity> findByClientId(Long clientId);

    List<TransactionJpaEntity> findByAccountIdIn(List<Long> accountIds);

    List<TransactionJpaEntity> findByTransferIdIn(List<Long> transferIds);

    List<TransactionJpaEntity> findByImportBatchId(Long importBatchId);

    boolean existsByAccountId(Long accountId);

    boolean existsByCategoryId(Long categoryId);

    boolean existsByClientId(Long clientId);

    @Query(
            "SELECT COALESCE(SUM(t.amount), 0) FROM TransactionJpaEntity t WHERE t.account.id = :accountId AND t.type = :type")
    BigDecimal sumAmountByAccountIdAndType(
            @Param("accountId") Long accountId, @Param("type") CategoryType type);

    @Query(
            """
        SELECT COALESCE(SUM(t.amount), 0) FROM TransactionJpaEntity t
        WHERE t.account.user.id = :userId AND t.category.id = :categoryId AND t.type = :type
          AND t.transferId IS NULL AND t.transactionDate >= :start AND t.transactionDate < :end
        """)
    BigDecimal sumAmountByUserIdAndCategoryIdAndTypeBetween(
            @Param("userId") Long userId,
            @Param("categoryId") Long categoryId,
            @Param("type") CategoryType type,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);
}
