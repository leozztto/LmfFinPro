package com.lmf.finpro.infrastructure.persistence.repository;

import com.lmf.finpro.infrastructure.persistence.entity.ImportBatchJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImportBatchJpaRepository extends JpaRepository<ImportBatchJpaEntity, Long> {
    List<ImportBatchJpaEntity> findByUserIdOrderByImportedAtDesc(Long userId);
}
