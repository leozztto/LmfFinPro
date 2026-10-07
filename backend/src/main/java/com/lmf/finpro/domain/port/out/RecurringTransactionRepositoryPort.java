package com.lmf.finpro.domain.port.out;

import com.lmf.finpro.domain.model.RecurringTransaction;
import java.util.List;
import java.util.Optional;

public interface RecurringTransactionRepositoryPort {
    RecurringTransaction save(RecurringTransaction recurringTransaction);

    Optional<RecurringTransaction> findById(Long id);

    List<RecurringTransaction> findAllByHouseholdId(Long householdId);

    List<RecurringTransaction> findAllActive();

    boolean existsByAccountId(Long accountId);

    void deleteById(Long id);
}
