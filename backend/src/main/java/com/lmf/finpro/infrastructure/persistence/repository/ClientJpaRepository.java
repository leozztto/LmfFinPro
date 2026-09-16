package com.lmf.finpro.infrastructure.persistence.repository;

import com.lmf.finpro.infrastructure.persistence.entity.ClientJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClientJpaRepository extends JpaRepository<ClientJpaEntity, Long> {
    List<ClientJpaEntity> findByUserId(Long userId);
}
