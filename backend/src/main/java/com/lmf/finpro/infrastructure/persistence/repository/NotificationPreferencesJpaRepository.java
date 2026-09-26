package com.lmf.finpro.infrastructure.persistence.repository;

import com.lmf.finpro.infrastructure.persistence.entity.NotificationPreferencesJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationPreferencesJpaRepository
        extends JpaRepository<NotificationPreferencesJpaEntity, Long> {}
