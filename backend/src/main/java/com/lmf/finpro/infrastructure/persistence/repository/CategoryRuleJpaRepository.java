package com.lmf.finpro.infrastructure.persistence.repository;

import com.lmf.finpro.infrastructure.persistence.entity.CategoryRuleJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRuleJpaRepository extends JpaRepository<CategoryRuleJpaEntity, Long> {
    List<CategoryRuleJpaEntity> findByUserIdOrderByWeightDesc(Long userId);
}
