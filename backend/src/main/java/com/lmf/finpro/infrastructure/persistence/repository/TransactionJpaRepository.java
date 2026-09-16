package com.lmf.finpro.infrastructure.persistence.repository;

import com.lmf.finpro.infrastructure.persistence.entity.TransactionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface TransactionJpaRepository extends JpaRepository<TransactionJpaEntity, Long> {
    List<TransactionJpaEntity> findByAccountIdAndTransactionDateBetween(Long accountId, LocalDate start, LocalDate end);
    List<TransactionJpaEntity> findByClientId(Long clientId);
    List<TransactionJpaEntity> findByAccountIdIn(List<Long> accountIds);
}
