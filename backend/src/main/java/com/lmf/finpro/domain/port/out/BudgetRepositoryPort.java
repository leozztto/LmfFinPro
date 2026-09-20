package com.lmf.finpro.domain.port.out;

import com.lmf.finpro.domain.model.Budget;
import java.util.List;
import java.util.Optional;

public interface BudgetRepositoryPort {
    Budget save(Budget budget);

    Optional<Budget> findById(Long id);

    List<Budget> findAllByUserId(Long userId);

    void deleteById(Long id);
}
