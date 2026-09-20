package com.lmf.finpro.domain.port.out;

import com.lmf.finpro.domain.model.ImportBatch;
import java.util.List;
import java.util.Optional;

public interface ImportBatchRepositoryPort {
    ImportBatch save(ImportBatch importBatch);

    Optional<ImportBatch> findById(Long id);

    List<ImportBatch> findAllByUserId(Long userId);
}
