package com.lmf.finpro.infrastructure.persistence.repository;

import com.lmf.finpro.infrastructure.persistence.entity.HouseholdMemberJpaEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HouseholdMemberJpaRepository
        extends JpaRepository<HouseholdMemberJpaEntity, Long> {
    Optional<HouseholdMemberJpaEntity> findByHouseholdIdAndUserId(Long householdId, Long userId);

    List<HouseholdMemberJpaEntity> findByUserId(Long userId);

    List<HouseholdMemberJpaEntity> findByHouseholdIdOrderByJoinedAtAsc(Long householdId);

    @Query(
            """
            SELECT m FROM HouseholdMemberJpaEntity m
            WHERE m.userId = :userId
              AND m.householdId IN (
                  SELECT h.id FROM HouseholdJpaEntity h
                  WHERE h.type = com.lmf.finpro.domain.model.HouseholdType.PERSONAL)
            """)
    Optional<HouseholdMemberJpaEntity> findPersonalByUserId(@Param("userId") Long userId);

    @Modifying
    @Query(
            "DELETE FROM HouseholdMemberJpaEntity m WHERE m.householdId = :householdId AND m.userId"
                    + " = :userId")
    void deleteByHouseholdIdAndUserId(
            @Param("householdId") Long householdId, @Param("userId") Long userId);
}
