package com.lmf.finpro.infrastructure.persistence.repository;

import com.lmf.finpro.infrastructure.persistence.entity.ClientJpaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientJpaRepository extends JpaRepository<ClientJpaEntity, Long> {
    List<ClientJpaEntity> findByUserId(Long userId);
}
