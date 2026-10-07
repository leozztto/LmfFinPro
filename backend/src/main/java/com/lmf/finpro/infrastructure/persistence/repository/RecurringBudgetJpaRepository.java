package com.lmf.finpro.infrastructure.persistence.repository;

import com.lmf.finpro.infrastructure.persistence.entity.RecurringBudgetJpaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecurringBudgetJpaRepository
        extends JpaRepository<RecurringBudgetJpaEntity, Long> {
    List<RecurringBudgetJpaEntity> findByHouseholdIdOrderByStartMonthAsc(Long householdId);

    List<RecurringBudgetJpaEntity> findByActiveTrue();

    boolean existsByCategoryId(Long categoryId);
}
