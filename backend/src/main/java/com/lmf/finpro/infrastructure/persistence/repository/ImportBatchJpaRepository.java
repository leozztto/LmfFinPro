package com.lmf.finpro.infrastructure.persistence.repository;

import com.lmf.finpro.infrastructure.persistence.entity.ImportBatchJpaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportBatchJpaRepository extends JpaRepository<ImportBatchJpaEntity, Long> {
    List<ImportBatchJpaEntity> findByUserIdOrderByImportedAtDesc(Long userId);
}
