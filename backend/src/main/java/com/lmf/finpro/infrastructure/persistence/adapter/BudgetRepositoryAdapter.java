package com.lmf.finpro.infrastructure.persistence.adapter;

import com.lmf.finpro.domain.model.Budget;
import com.lmf.finpro.domain.port.out.BudgetRepositoryPort;
import com.lmf.finpro.infrastructure.persistence.mapper.BudgetPersistenceMapper;
import com.lmf.finpro.infrastructure.persistence.repository.BudgetJpaRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BudgetRepositoryAdapter implements BudgetRepositoryPort {

    private final BudgetJpaRepository budgetJpaRepository;
    private final BudgetPersistenceMapper mapper;

    @Override
    public Budget save(Budget budget) {
        return mapper.toDomain(budgetJpaRepository.save(mapper.toEntity(budget)));
    }

    @Override
    public Optional<Budget> findById(Long id) {
        return budgetJpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Budget> findAllByUserId(Long userId) {
        return budgetJpaRepository.findByUserId(userId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public void deleteById(Long id) {
        budgetJpaRepository.deleteById(id);
    }
}
