package com.lmf.finpro.repository;

import com.lmf.finpro.domain.CategoryRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRuleRepository extends JpaRepository<CategoryRule, Long> {
    List<CategoryRule> findByUserIdOrderByWeightDesc(Long userId);
}
