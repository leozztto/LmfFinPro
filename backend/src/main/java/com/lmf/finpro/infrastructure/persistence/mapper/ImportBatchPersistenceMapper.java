package com.lmf.finpro.infrastructure.persistence.mapper;

import com.lmf.finpro.domain.model.ImportBatch;
import com.lmf.finpro.infrastructure.persistence.entity.AccountJpaEntity;
import com.lmf.finpro.infrastructure.persistence.entity.ImportBatchJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class ImportBatchPersistenceMapper {

    public ImportBatchJpaEntity toEntity(ImportBatch importBatch) {
        return ImportBatchJpaEntity.builder()
                .id(importBatch.id())
                .householdId(importBatch.householdId())
                .account(AccountJpaEntity.builder().id(importBatch.accountId()).build())
                .originalFile(importBatch.originalFile())
                .format(importBatch.format())
                .importedAt(importBatch.importedAt())
                .status(importBatch.status())
                .duplicateCount(importBatch.duplicateCount())
                .build();
    }

    public ImportBatch toDomain(ImportBatchJpaEntity entity) {
        return new ImportBatch(
                entity.getId(),
                entity.getHouseholdId(),
                entity.getAccount().getId(),
                entity.getOriginalFile(),
                entity.getFormat(),
                entity.getImportedAt(),
                entity.getStatus(),
                entity.getDuplicateCount());
    }
}
