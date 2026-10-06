package com.lmf.finpro.infrastructure.persistence.repository;

import com.lmf.finpro.infrastructure.persistence.entity.SavingsGoalJpaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SavingsGoalJpaRepository extends JpaRepository<SavingsGoalJpaEntity, Long> {
    List<SavingsGoalJpaEntity> findByHouseholdIdOrderByCreatedAtAsc(Long householdId);

    List<SavingsGoalJpaEntity> findByAutoContributeTrue();

    boolean existsByAccountIdOrFundingAccountId(Long accountId, Long fundingAccountId);
}
