package com.lmf.finpro.infrastructure.persistence.repository;

import com.lmf.finpro.infrastructure.persistence.entity.HouseholdInviteJpaEntity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HouseholdInviteJpaRepository
        extends JpaRepository<HouseholdInviteJpaEntity, Long> {
    Optional<HouseholdInviteJpaEntity> findByTokenHash(String tokenHash);

    @Query(
            """
            SELECT i FROM HouseholdInviteJpaEntity i
            WHERE i.householdId = :householdId AND i.acceptedAt IS NULL AND i.expiresAt > :now
            ORDER BY i.createdAt DESC
            """)
    List<HouseholdInviteJpaEntity> findPending(
            @Param("householdId") Long householdId, @Param("now") LocalDateTime now);

    @Query(
            """
            SELECT i FROM HouseholdInviteJpaEntity i
            WHERE lower(i.email) = lower(:email) AND i.acceptedAt IS NULL AND i.expiresAt > :now
            ORDER BY i.createdAt DESC
            """)
    List<HouseholdInviteJpaEntity> findPendingByEmail(
            @Param("email") String email, @Param("now") LocalDateTime now);

    @Modifying
    @Query(
            """
            DELETE FROM HouseholdInviteJpaEntity i
            WHERE i.householdId = :householdId AND lower(i.email) = lower(:email)
              AND i.acceptedAt IS NULL AND i.expiresAt > :now
            """)
    void deletePending(
            @Param("householdId") Long householdId,
            @Param("email") String email,
            @Param("now") LocalDateTime now);
}
