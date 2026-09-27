package com.lmf.finpro.infrastructure.persistence.adapter;

import com.lmf.finpro.domain.model.Tag;
import com.lmf.finpro.domain.port.out.TagRepositoryPort;
import com.lmf.finpro.infrastructure.persistence.entity.RecurringTransactionTagJpaEntity;
import com.lmf.finpro.infrastructure.persistence.entity.TagJpaEntity;
import com.lmf.finpro.infrastructure.persistence.entity.TransactionTagJpaEntity;
import com.lmf.finpro.infrastructure.persistence.repository.RecurringTransactionTagJpaRepository;
import com.lmf.finpro.infrastructure.persistence.repository.TagJpaRepository;
import com.lmf.finpro.infrastructure.persistence.repository.TransactionTagJpaRepository;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class TagRepositoryAdapter implements TagRepositoryPort {

    private final TagJpaRepository tagRepository;
    private final TransactionTagJpaRepository transactionTagRepository;
    private final RecurringTransactionTagJpaRepository recurringTransactionTagRepository;

    @Override
    public Tag save(Tag tag) {
        return toDomain(
                tagRepository.save(
                        TagJpaEntity.builder()
                                .id(tag.id())
                                .userId(tag.userId())
                                .name(tag.name())
                                .color(tag.color())
                                .createdAt(tag.createdAt())
                                .build()));
    }

    @Override
    public Optional<Tag> findById(Long id) {
        return tagRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Tag> findAllByUserId(Long userId) {
        return tagRepository.findByUserIdOrderByNameAsc(userId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Tag> findAllByUserIdAndNames(Long userId, Collection<String> names) {
        if (names.isEmpty()) {
            return List.of();
        }
        return tagRepository.findByUserIdAndNameIn(userId, names).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void deleteById(Long id) {
        tagRepository.deleteById(id);
    }

    @Override
    public Map<Long, List<Long>> findTagIdsByTransactionIds(Collection<Long> transactionIds) {
        if (transactionIds.isEmpty()) {
            return Map.of();
        }
        return transactionTagRepository.findByTransactionIdIn(transactionIds).stream()
                .collect(
                        Collectors.groupingBy(
                                TransactionTagJpaEntity::getTransactionId,
                                Collectors.mapping(
                                        TransactionTagJpaEntity::getTagId, Collectors.toList())));
    }

    @Override
    @Transactional
    public void replaceTransactionTags(Long transactionId, Collection<Long> tagIds) {
        transactionTagRepository.deleteByTransactionId(transactionId);
        transactionTagRepository.saveAll(
                tagIds.stream()
                        .distinct()
                        .map(tagId -> new TransactionTagJpaEntity(transactionId, tagId))
                        .toList());
    }

    @Override
    public Map<Long, Long> countTransactionsByTagIds(Collection<Long> tagIds) {
        if (tagIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : transactionTagRepository.countGroupedByTagId(tagIds)) {
            counts.put((Long) row[0], (Long) row[1]);
        }
        return counts;
    }

    @Override
    public Map<Long, List<Long>> findTagIdsByRecurringTransactionIds(
            Collection<Long> recurringTransactionIds) {
        if (recurringTransactionIds.isEmpty()) {
            return Map.of();
        }
        return recurringTransactionTagRepository
                .findByRecurringTransactionIdIn(recurringTransactionIds)
                .stream()
                .collect(
                        Collectors.groupingBy(
                                RecurringTransactionTagJpaEntity::getRecurringTransactionId,
                                Collectors.mapping(
                                        RecurringTransactionTagJpaEntity::getTagId,
                                        Collectors.toList())));
    }

    @Override
    @Transactional
    public void replaceRecurringTransactionTags(
            Long recurringTransactionId, Collection<Long> tagIds) {
        recurringTransactionTagRepository.deleteByRecurringTransactionId(recurringTransactionId);
        recurringTransactionTagRepository.saveAll(
                tagIds.stream()
                        .distinct()
                        .map(
                                tagId ->
                                        new RecurringTransactionTagJpaEntity(
                                                recurringTransactionId, tagId))
                        .toList());
    }

    private Tag toDomain(TagJpaEntity entity) {
        return new Tag(
                entity.getId(),
                entity.getUserId(),
                entity.getName(),
                entity.getColor(),
                entity.getCreatedAt());
    }
}
