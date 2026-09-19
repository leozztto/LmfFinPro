package com.lmf.finpro.infrastructure.persistence.adapter;

import com.lmf.finpro.domain.model.ImportBatch;
import com.lmf.finpro.domain.port.out.ImportBatchRepositoryPort;
import com.lmf.finpro.infrastructure.persistence.mapper.ImportBatchPersistenceMapper;
import com.lmf.finpro.infrastructure.persistence.repository.ImportBatchJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ImportBatchRepositoryAdapter implements ImportBatchRepositoryPort {

    private final ImportBatchJpaRepository importBatchJpaRepository;
    private final ImportBatchPersistenceMapper mapper;

    @Override
    public ImportBatch save(ImportBatch importBatch) {
        return mapper.toDomain(importBatchJpaRepository.save(mapper.toEntity(importBatch)));
    }

    @Override
    public Optional<ImportBatch> findById(Long id) {
        return importBatchJpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<ImportBatch> findAllByUserId(Long userId) {
        return importBatchJpaRepository.findByUserIdOrderByImportedAtDesc(userId).stream()
            .map(mapper::toDomain)
            .toList();
    }
}
