package com.lmf.finpro.infrastructure.persistence.repository;

import com.lmf.finpro.infrastructure.persistence.entity.GoalContributionJpaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoalContributionJpaRepository
        extends JpaRepository<GoalContributionJpaEntity, Long> {
    List<GoalContributionJpaEntity> findByGoalIdOrderByContributionDateDescIdDesc(Long goalId);
}
