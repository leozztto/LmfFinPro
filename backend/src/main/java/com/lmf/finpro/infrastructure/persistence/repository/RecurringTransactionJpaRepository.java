package com.lmf.finpro.infrastructure.persistence.repository;

import com.lmf.finpro.infrastructure.persistence.entity.RecurringTransactionJpaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecurringTransactionJpaRepository
        extends JpaRepository<RecurringTransactionJpaEntity, Long> {
    List<RecurringTransactionJpaEntity> findByUserIdOrderByStartDateAsc(Long userId);

    List<RecurringTransactionJpaEntity> findByActiveTrue();

    boolean existsByAccountId(Long accountId);
}
