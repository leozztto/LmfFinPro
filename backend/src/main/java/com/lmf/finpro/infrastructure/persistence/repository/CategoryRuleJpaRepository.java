package com.lmf.finpro.infrastructure.persistence.repository;

import com.lmf.finpro.infrastructure.persistence.entity.CategoryRuleJpaEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRuleJpaRepository extends JpaRepository<CategoryRuleJpaEntity, Long> {
    List<CategoryRuleJpaEntity> findByUserIdOrderByWeightDesc(Long userId);

    Optional<CategoryRuleJpaEntity> findByUserIdAndPatternIgnoreCase(Long userId, String pattern);
}
