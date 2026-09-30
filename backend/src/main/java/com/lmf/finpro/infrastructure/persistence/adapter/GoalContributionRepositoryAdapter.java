package com.lmf.finpro.infrastructure.persistence.adapter;

import com.lmf.finpro.domain.model.GoalContribution;
import com.lmf.finpro.domain.port.out.GoalContributionRepositoryPort;
import com.lmf.finpro.infrastructure.persistence.entity.GoalContributionJpaEntity;
import com.lmf.finpro.infrastructure.persistence.repository.GoalContributionJpaRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GoalContributionRepositoryAdapter implements GoalContributionRepositoryPort {

    private final GoalContributionJpaRepository repository;

    @Override
    public GoalContribution save(GoalContribution contribution) {
        return toDomain(
                repository.save(
                        GoalContributionJpaEntity.builder()
                                .id(contribution.id())
                                .goalId(contribution.goalId())
                                .type(contribution.type())
                                .amount(contribution.amount())
                                .contributionDate(contribution.contributionDate())
                                .note(contribution.note())
                                .createdAt(contribution.createdAt())
                                .transferId(contribution.transferId())
                                .build()));
    }

    @Override
    public Optional<GoalContribution> findById(Long id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public List<GoalContribution> findAllByGoalId(Long goalId) {
        return repository.findByGoalIdOrderByContributionDateDescIdDesc(goalId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    private GoalContribution toDomain(GoalContributionJpaEntity entity) {
        return new GoalContribution(
                entity.getId(),
                entity.getGoalId(),
                entity.getType(),
                entity.getAmount(),
                entity.getContributionDate(),
                entity.getNote(),
                entity.getCreatedAt(),
                entity.getTransferId());
    }
}
