package com.lmf.finpro.infrastructure.persistence.adapter;

import com.lmf.finpro.domain.model.SavingsGoal;
import com.lmf.finpro.domain.port.out.SavingsGoalRepositoryPort;
import com.lmf.finpro.infrastructure.persistence.entity.SavingsGoalJpaEntity;
import com.lmf.finpro.infrastructure.persistence.repository.SavingsGoalJpaRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SavingsGoalRepositoryAdapter implements SavingsGoalRepositoryPort {

    private final SavingsGoalJpaRepository repository;

    @Override
    public SavingsGoal save(SavingsGoal goal) {
        return toDomain(
                repository.save(
                        SavingsGoalJpaEntity.builder()
                                .id(goal.id())
                                .userId(goal.userId())
                                .name(goal.name())
                                .type(goal.type())
                                .targetAmount(goal.targetAmount())
                                .deadline(goal.deadline())
                                .incomeRate(goal.incomeRate())
                                .createdAt(goal.createdAt())
                                .autoContribute(goal.autoContribute())
                                .build()));
    }

    @Override
    public Optional<SavingsGoal> findById(Long id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public List<SavingsGoal> findAllByUserId(Long userId) {
        return repository.findByUserIdOrderByCreatedAtAsc(userId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<SavingsGoal> findAllAutoContribute() {
        return repository.findByAutoContributeTrue().stream().map(this::toDomain).toList();
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    private SavingsGoal toDomain(SavingsGoalJpaEntity entity) {
        return new SavingsGoal(
                entity.getId(),
                entity.getUserId(),
                entity.getName(),
                entity.getType(),
                entity.getTargetAmount(),
                entity.getDeadline(),
                entity.getIncomeRate(),
                entity.getCreatedAt(),
                entity.isAutoContribute());
    }
}
