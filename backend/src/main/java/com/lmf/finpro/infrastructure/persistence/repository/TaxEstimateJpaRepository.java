package com.lmf.finpro.infrastructure.persistence.repository;

import com.lmf.finpro.infrastructure.persistence.entity.TaxEstimateJpaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaxEstimateJpaRepository extends JpaRepository<TaxEstimateJpaEntity, Long> {
    List<TaxEstimateJpaEntity> findByUserId(Long userId);
}
