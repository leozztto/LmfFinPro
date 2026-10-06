package com.lmf.finpro.domain.port.out;

import com.lmf.finpro.domain.model.SavingsGoal;
import java.util.List;
import java.util.Optional;

public interface SavingsGoalRepositoryPort {
    SavingsGoal save(SavingsGoal goal);

    Optional<SavingsGoal> findById(Long id);

    List<SavingsGoal> findAllByHouseholdId(Long householdId);

    /** Metas com aporte automático ligado, de todos os usuários — usado pelo scheduler diário. */
    List<SavingsGoal> findAllAutoContribute();

    /** Conta usada como reserva ou como origem de aportes por alguma meta. */
    boolean existsByAccountId(Long accountId);

    /** Os aportes da meta são removidos junto (FK ON DELETE CASCADE). */
    void deleteById(Long id);
}
