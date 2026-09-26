package com.lmf.finpro.domain.port.out;

import com.lmf.finpro.domain.model.GoalContribution;
import java.util.List;
import java.util.Optional;

public interface GoalContributionRepositoryPort {
    GoalContribution save(GoalContribution contribution);

    Optional<GoalContribution> findById(Long id);

    /** Mais recentes primeiro. */
    List<GoalContribution> findAllByGoalId(Long goalId);

    void deleteById(Long id);
}
