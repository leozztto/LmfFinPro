package com.lmf.finpro.infrastructure.persistence.repository;

import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.infrastructure.persistence.entity.TransactionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface TransactionJpaRepository extends JpaRepository<TransactionJpaEntity, Long> {
    List<TransactionJpaEntity> findByAccountIdAndTransactionDateBetween(Long accountId, LocalDate start, LocalDate end);
    List<TransactionJpaEntity> findByClientId(Long clientId);
    List<TransactionJpaEntity> findByAccountIdIn(List<Long> accountIds);
    List<TransactionJpaEntity> findByTransferIdIn(List<Long> transferIds);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM TransactionJpaEntity t WHERE t.account.id = :accountId AND t.type = :type")
    BigDecimal sumAmountByAccountIdAndType(@Param("accountId") Long accountId, @Param("type") CategoryType type);
}
