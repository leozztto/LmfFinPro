package com.lmf.finpro.domain.port.out;

import com.lmf.finpro.domain.model.RecurringBudget;
import java.util.List;
import java.util.Optional;

public interface RecurringBudgetRepositoryPort {
    RecurringBudget save(RecurringBudget recurringBudget);

    Optional<RecurringBudget> findById(Long id);

    List<RecurringBudget> findAllByUserId(Long userId);

    List<RecurringBudget> findAllActive();

    boolean existsByCategoryId(Long categoryId);

    void deleteById(Long id);
}
