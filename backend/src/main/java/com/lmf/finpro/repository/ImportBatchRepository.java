package com.lmf.finpro.repository;

import com.lmf.finpro.domain.ImportBatch;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportBatchRepository extends JpaRepository<ImportBatch, Long> {
}
