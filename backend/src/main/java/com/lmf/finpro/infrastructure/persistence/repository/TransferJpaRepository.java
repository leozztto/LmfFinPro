package com.lmf.finpro.infrastructure.persistence.repository;

import com.lmf.finpro.infrastructure.persistence.entity.TransferJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransferJpaRepository extends JpaRepository<TransferJpaEntity, Long> {
    List<TransferJpaEntity> findByUserId(Long userId);
}
