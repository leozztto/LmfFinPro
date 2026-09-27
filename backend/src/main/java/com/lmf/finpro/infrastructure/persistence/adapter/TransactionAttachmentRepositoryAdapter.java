package com.lmf.finpro.infrastructure.persistence.adapter;

import com.lmf.finpro.domain.model.TransactionAttachment;
import com.lmf.finpro.domain.port.out.TransactionAttachmentRepositoryPort;
import com.lmf.finpro.infrastructure.persistence.entity.TransactionAttachmentJpaEntity;
import com.lmf.finpro.infrastructure.persistence.repository.TransactionAttachmentJpaRepository;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TransactionAttachmentRepositoryAdapter implements TransactionAttachmentRepositoryPort {

    private final TransactionAttachmentJpaRepository repository;

    @Override
    public TransactionAttachment save(TransactionAttachment attachment) {
        return toDomain(
                repository.save(
                        TransactionAttachmentJpaEntity.builder()
                                .id(attachment.id())
                                .transactionId(attachment.transactionId())
                                .userId(attachment.userId())
                                .documentType(attachment.documentType())
                                .fileName(attachment.fileName())
                                .contentType(attachment.contentType())
                                .sizeBytes(attachment.sizeBytes())
                                .storageKey(attachment.storageKey())
                                .createdAt(attachment.createdAt())
                                .build()));
    }

    @Override
    public Optional<TransactionAttachment> findById(Long id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public List<TransactionAttachment> findAllByTransactionId(Long transactionId) {
        return repository.findByTransactionIdOrderByCreatedAtAscIdAsc(transactionId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<TransactionAttachment> findAllByTransactionIds(Collection<Long> transactionIds) {
        if (transactionIds.isEmpty()) {
            return List.of();
        }
        return repository.findByTransactionIdIn(transactionIds).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Map<Long, Long> countByTransactionIds(Collection<Long> transactionIds) {
        Map<Long, Long> counts = new HashMap<>();
        if (transactionIds.isEmpty()) {
            return counts;
        }
        for (Object[] row : repository.countGroupedByTransactionId(transactionIds)) {
            counts.put((Long) row[0], (Long) row[1]);
        }
        return counts;
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    private TransactionAttachment toDomain(TransactionAttachmentJpaEntity entity) {
        return new TransactionAttachment(
                entity.getId(),
                entity.getTransactionId(),
                entity.getUserId(),
                entity.getDocumentType(),
                entity.getFileName(),
                entity.getContentType(),
                entity.getSizeBytes(),
                entity.getStorageKey(),
                entity.getCreatedAt());
    }
}
