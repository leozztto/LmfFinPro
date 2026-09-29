package com.lmf.finpro.infrastructure.persistence.repository;

import com.lmf.finpro.infrastructure.persistence.entity.BudgetJpaEntity;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BudgetJpaRepository extends JpaRepository<BudgetJpaEntity, Long> {
    List<BudgetJpaEntity> findByUserId(Long userId);

    boolean existsByUserIdAndCategoryIdAndReferenceMonth(
            Long userId, Long categoryId, LocalDate referenceMonth);

    boolean existsByCategoryId(Long categoryId);
}
