package com.lmf.finpro.domain.port.out;

import com.lmf.finpro.domain.model.CategoryRule;
import java.util.List;
import java.util.Optional;

public interface CategoryRuleRepositoryPort {
    CategoryRule save(CategoryRule categoryRule);

    Optional<CategoryRule> findById(Long id);

    List<CategoryRule> findAllByUserIdOrderByWeightDesc(Long userId);

    Optional<CategoryRule> findByUserIdAndPattern(Long userId, String pattern);

    void deleteById(Long id);
}
