package com.lmf.finpro.application.categoryrule;

import com.lmf.finpro.domain.exception.ResourceNotFoundException;
import com.lmf.finpro.domain.model.CategoryRule;
import com.lmf.finpro.domain.port.out.CategoryRepositoryPort;
import com.lmf.finpro.domain.port.out.CategoryRuleRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryRuleApplicationService {

    private final CategoryRuleRepositoryPort categoryRuleRepositoryPort;
    private final CategoryRepositoryPort categoryRepositoryPort;

    public CategoryRule create(Long currentUserId, String pattern, Long categoryId) {
        requireVisibleCategory(currentUserId, categoryId);
        return categoryRuleRepositoryPort.save(CategoryRule.create(currentUserId, pattern.trim(), categoryId));
    }

    public List<CategoryRule> list(Long currentUserId) {
        return categoryRuleRepositoryPort.findAllByUserIdOrderByWeightDesc(currentUserId);
    }

    public void delete(Long currentUserId, Long ruleId) {
        CategoryRule rule = findOwnedOrThrow(currentUserId, ruleId);
        categoryRuleRepositoryPort.deleteById(rule.id());
    }

    private CategoryRule findOwnedOrThrow(Long currentUserId, Long ruleId) {
        return categoryRuleRepositoryPort.findById(ruleId)
            .filter(rule -> rule.belongsTo(currentUserId))
            .orElseThrow(() -> new ResourceNotFoundException("Regra não encontrada: " + ruleId));
    }

    private void requireVisibleCategory(Long currentUserId, Long categoryId) {
        categoryRepositoryPort.findById(categoryId)
            .filter(candidate -> candidate.isVisibleTo(currentUserId))
            .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada: " + categoryId));
    }
}
