package com.lmf.finpro.infrastructure.persistence.adapter;

import com.lmf.finpro.domain.model.CategoryRule;
import com.lmf.finpro.domain.port.out.CategoryRuleRepositoryPort;
import com.lmf.finpro.infrastructure.persistence.mapper.CategoryRulePersistenceMapper;
import com.lmf.finpro.infrastructure.persistence.repository.CategoryRuleJpaRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CategoryRuleRepositoryAdapter implements CategoryRuleRepositoryPort {

    private final CategoryRuleJpaRepository categoryRuleJpaRepository;
    private final CategoryRulePersistenceMapper mapper;

    @Override
    public CategoryRule save(CategoryRule categoryRule) {
        return mapper.toDomain(categoryRuleJpaRepository.save(mapper.toEntity(categoryRule)));
    }

    @Override
    public Optional<CategoryRule> findById(Long id) {
        return categoryRuleJpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<CategoryRule> findAllByUserIdOrderByWeightDesc(Long userId) {
        return categoryRuleJpaRepository.findByUserIdOrderByWeightDesc(userId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<CategoryRule> findByUserIdAndPattern(Long userId, String pattern) {
        return categoryRuleJpaRepository
                .findByUserIdAndPatternIgnoreCase(userId, pattern)
                .map(mapper::toDomain);
    }

    @Override
    public void deleteById(Long id) {
        categoryRuleJpaRepository.deleteById(id);
    }
}
