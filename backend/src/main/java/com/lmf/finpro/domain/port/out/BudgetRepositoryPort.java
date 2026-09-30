package com.lmf.finpro.domain.port.out;

import com.lmf.finpro.domain.model.Budget;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public interface BudgetRepositoryPort {
    Budget save(Budget budget);

    Optional<Budget> findById(Long id);

    List<Budget> findAllByUserId(Long userId);

    /** {@code clientId} nulo procura o orçamento geral (sem cliente) da categoria/mês. */
    boolean existsByUserIdAndCategoryIdAndReferenceMonthAndClientId(
            Long userId, Long categoryId, YearMonth referenceMonth, Long clientId);

    boolean existsByCategoryId(Long categoryId);

    boolean existsByClientId(Long clientId);

    void deleteById(Long id);
}
