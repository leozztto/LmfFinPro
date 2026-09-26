package com.lmf.finpro.infrastructure.persistence.repository;

import com.lmf.finpro.infrastructure.persistence.entity.ProLaboreSettingsJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProLaboreSettingsJpaRepository
        extends JpaRepository<ProLaboreSettingsJpaEntity, Long> {}
