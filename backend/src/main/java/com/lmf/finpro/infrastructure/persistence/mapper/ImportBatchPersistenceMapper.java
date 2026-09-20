package com.lmf.finpro.infrastructure.persistence.mapper;

import com.lmf.finpro.domain.model.ImportBatch;
import com.lmf.finpro.infrastructure.persistence.entity.AccountJpaEntity;
import com.lmf.finpro.infrastructure.persistence.entity.ImportBatchJpaEntity;
import com.lmf.finpro.infrastructure.persistence.entity.UserJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class ImportBatchPersistenceMapper {

    public ImportBatchJpaEntity toEntity(ImportBatch importBatch) {
        return ImportBatchJpaEntity.builder()
            .id(importBatch.id())
            .user(UserJpaEntity.builder().id(importBatch.userId()).build())
            .account(AccountJpaEntity.builder().id(importBatch.accountId()).build())
            .originalFile(importBatch.originalFile())
            .format(importBatch.format())
            .importedAt(importBatch.importedAt())
            .status(importBatch.status())
            .build();
    }

    public ImportBatch toDomain(ImportBatchJpaEntity entity) {
        return new ImportBatch(
            entity.getId(),
            entity.getUser().getId(),
            entity.getAccount().getId(),
            entity.getOriginalFile(),
            entity.getFormat(),
            entity.getImportedAt(),
            entity.getStatus()
        );
    }
}
