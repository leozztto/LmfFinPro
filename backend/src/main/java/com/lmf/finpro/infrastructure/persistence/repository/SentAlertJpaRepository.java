package com.lmf.finpro.infrastructure.persistence.repository;

import com.lmf.finpro.domain.model.AlertType;
import com.lmf.finpro.infrastructure.persistence.entity.SentAlertJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SentAlertJpaRepository extends JpaRepository<SentAlertJpaEntity, Long> {
    boolean existsByUserIdAndAlertTypeAndReferenceKey(
            Long userId, AlertType alertType, String referenceKey);
}
