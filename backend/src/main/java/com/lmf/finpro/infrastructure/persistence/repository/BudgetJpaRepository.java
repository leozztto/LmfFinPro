package com.lmf.finpro.infrastructure.persistence.repository;

import com.lmf.finpro.infrastructure.persistence.entity.BudgetJpaEntity;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BudgetJpaRepository extends JpaRepository<BudgetJpaEntity, Long> {
    List<BudgetJpaEntity> findByHouseholdId(Long householdId);

    // clientId nulo vira "client IS NULL" na query derivada: procura o orçamento geral.
    boolean existsByHouseholdIdAndCategoryIdAndReferenceMonthAndClientId(
            Long householdId, Long categoryId, LocalDate referenceMonth, Long clientId);

    boolean existsByCategoryId(Long categoryId);

    boolean existsByClientId(Long clientId);
}
