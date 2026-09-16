package com.lmf.finpro.infrastructure.persistence.repository;

import com.lmf.finpro.infrastructure.persistence.entity.BudgetJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BudgetJpaRepository extends JpaRepository<BudgetJpaEntity, Long> {
}
