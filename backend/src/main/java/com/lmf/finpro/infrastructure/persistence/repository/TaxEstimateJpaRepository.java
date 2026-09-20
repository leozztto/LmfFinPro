package com.lmf.finpro.infrastructure.persistence.repository;

import com.lmf.finpro.infrastructure.persistence.entity.TaxEstimateJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaxEstimateJpaRepository extends JpaRepository<TaxEstimateJpaEntity, Long> {
    List<TaxEstimateJpaEntity> findByUserId(Long userId);
}
