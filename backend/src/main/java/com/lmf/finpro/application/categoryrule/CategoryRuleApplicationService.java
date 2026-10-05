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

    public CategoryRule create(Long currentUserId, String pattern, Long categoryId) {
        log.debug("Criando regra para a categoria={} do usuário={}", categoryId, currentUserId);
        requireVisibleCategory(currentUserId, categoryId);
        CategoryRule saved =
                categoryRuleRepositoryPort.save(
                        CategoryRule.create(currentUserId, pattern.trim(), categoryId));
        log.debug("Regra={} criada para a categoria={}", saved.id(), categoryId);
        return saved;
    }

    public List<CategoryRule> list(Long currentUserId) {
        log.debug("Listando regras de categoria do usuário={}", currentUserId);
        return categoryRuleRepositoryPort.findVisibleToUserOrderByPriorityDesc(currentUserId);
    }

    public void delete(Long currentUserId, Long ruleId) {
        CategoryRule rule = findOwnedOrThrow(currentUserId, ruleId);
        log.debug(
                "Removendo regra={} da categoria={} do usuário={}",
                ruleId,
                rule.categoryId(),
                currentUserId);
        categoryRuleRepositoryPort.deleteById(rule.id());
    }

    private CategoryRule findOwnedOrThrow(Long currentUserId, Long ruleId) {
        return categoryRuleRepositoryPort
                .findById(ruleId)
                .filter(rule -> rule.belongsTo(currentUserId))
                .orElseThrow(
                        () -> new ResourceNotFoundException("Regra não encontrada: " + ruleId));
    }

    private void requireVisibleCategory(Long currentUserId, Long categoryId) {
        categoryRepositoryPort
                .findById(categoryId)
                .filter(candidate -> candidate.isVisibleTo(currentUserId))
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Categoria não encontrada: " + categoryId));
    }
}
