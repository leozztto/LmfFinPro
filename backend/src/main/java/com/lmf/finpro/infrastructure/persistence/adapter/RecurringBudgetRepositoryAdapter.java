package com.lmf.finpro.infrastructure.persistence.adapter;

import com.lmf.finpro.domain.model.RecurringBudget;
import com.lmf.finpro.domain.port.out.RecurringBudgetRepositoryPort;
import com.lmf.finpro.infrastructure.persistence.mapper.RecurringBudgetPersistenceMapper;
import com.lmf.finpro.infrastructure.persistence.repository.RecurringBudgetJpaRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RecurringBudgetRepositoryAdapter implements RecurringBudgetRepositoryPort {

    private final RecurringBudgetJpaRepository recurringBudgetJpaRepository;
    private final RecurringBudgetPersistenceMapper mapper;

    @Override
    public RecurringBudget save(RecurringBudget recurringBudget) {
        return mapper.toDomain(recurringBudgetJpaRepository.save(mapper.toEntity(recurringBudget)));
    }

    @Override
    public Optional<RecurringBudget> findById(Long id) {
        return recurringBudgetJpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<RecurringBudget> findAllByHouseholdId(Long householdId) {
        return recurringBudgetJpaRepository
                .findByHouseholdIdOrderByStartMonthAsc(householdId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<RecurringBudget> findAllActive() {
        return recurringBudgetJpaRepository.findByActiveTrue().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByCategoryId(Long categoryId) {
        return recurringBudgetJpaRepository.existsByCategoryId(categoryId);
    }

    @Override
    public void deleteById(Long id) {
        recurringBudgetJpaRepository.deleteById(id);
    }
}
