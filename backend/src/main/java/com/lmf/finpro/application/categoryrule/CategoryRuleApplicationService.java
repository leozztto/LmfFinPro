package com.lmf.finpro.application.categoryrule;

import com.lmf.finpro.domain.exception.ResourceNotFoundException;
import com.lmf.finpro.domain.model.CategoryRule;
import com.lmf.finpro.domain.port.out.CategoryRepositoryPort;
import com.lmf.finpro.domain.port.out.CategoryRuleRepositoryPort;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryRuleApplicationService {

    private final CategoryRuleRepositoryPort categoryRuleRepositoryPort;
    private final CategoryRepositoryPort categoryRepositoryPort;

    public CategoryRule create(Long currentHouseholdId, String pattern, Long categoryId) {
        log.debug(
                "Criando regra para a categoria={} do usuário={}", categoryId, currentHouseholdId);
        requireVisibleCategory(currentHouseholdId, categoryId);
        CategoryRule saved =
                categoryRuleRepositoryPort.save(
                        CategoryRule.create(currentHouseholdId, pattern.trim(), categoryId));
        log.debug("Regra={} criada para a categoria={}", saved.id(), categoryId);
        return saved;
    }

    public List<CategoryRule> list(Long currentHouseholdId) {
        log.debug("Listando regras de categoria do usuário={}", currentHouseholdId);
        return categoryRuleRepositoryPort.findVisibleToUserOrderByPriorityDesc(currentHouseholdId);
    }

    public void delete(Long currentHouseholdId, Long ruleId) {
        CategoryRule rule = findOwnedOrThrow(currentHouseholdId, ruleId);
        log.debug(
                "Removendo regra={} da categoria={} do usuário={}",
                ruleId,
                rule.categoryId(),
                currentHouseholdId);
        categoryRuleRepositoryPort.deleteById(rule.id());
    }

    private CategoryRule findOwnedOrThrow(Long currentHouseholdId, Long ruleId) {
        return categoryRuleRepositoryPort
                .findById(ruleId)
                .filter(rule -> rule.belongsTo(currentHouseholdId))
                .orElseThrow(
                        () -> new ResourceNotFoundException("Regra não encontrada: " + ruleId));
    }

    private void requireVisibleCategory(Long currentHouseholdId, Long categoryId) {
        categoryRepositoryPort
                .findById(categoryId)
                .filter(candidate -> candidate.isVisibleTo(currentHouseholdId))
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Categoria não encontrada: " + categoryId));
    }
}
