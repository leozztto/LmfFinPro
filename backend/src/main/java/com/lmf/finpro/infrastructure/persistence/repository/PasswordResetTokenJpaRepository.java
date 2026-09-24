package com.lmf.finpro.infrastructure.persistence.repository;

import com.lmf.finpro.infrastructure.persistence.entity.PasswordResetTokenJpaEntity;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PasswordResetTokenJpaRepository
        extends JpaRepository<PasswordResetTokenJpaEntity, Long> {
    Optional<PasswordResetTokenJpaEntity> findByTokenHash(String tokenHash);

    @Modifying
    @Query(
            "UPDATE PasswordResetTokenJpaEntity t SET t.usedAt = :usedAt"
                    + " WHERE t.userId = :userId AND t.usedAt IS NULL")
    void invalidateActiveTokens(
            @Param("userId") Long userId, @Param("usedAt") LocalDateTime usedAt);
}
