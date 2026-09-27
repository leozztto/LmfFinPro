package com.lmf.finpro.application.tag;

import com.lmf.finpro.domain.exception.InvalidTagException;
import com.lmf.finpro.domain.exception.ResourceNotFoundException;
import com.lmf.finpro.domain.exception.TagAlreadyExistsException;
import com.lmf.finpro.domain.model.Tag;
import com.lmf.finpro.domain.port.out.TagRepositoryPort;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tags do usuário e os vínculos delas com transações e recorrências. As telas mandam os
 * <b>nomes</b> das tags; aqui eles são normalizados e cada um vira a tag existente ou uma nova —
 * assim "#Site Acme" e "site-acme" sempre caem na mesma tag.
 */
@Service
@RequiredArgsConstructor
public class TagApplicationService {

    private final TagRepositoryPort tagRepositoryPort;

    /** Uma tag com quantas transações a usam (para a tela de gestão). */
    public record TagUsage(Tag tag, long transactionCount) {}

    public List<TagUsage> list(Long currentUserId) {
        List<Tag> tags = tagRepositoryPort.findAllByUserId(currentUserId);
        Map<Long, Long> counts =
                tagRepositoryPort.countTransactionsByTagIds(tags.stream().map(Tag::id).toList());
        return tags.stream()
                .map(tag -> new TagUsage(tag, counts.getOrDefault(tag.id(), 0L)))
                .toList();
    }

    public Tag create(Long currentUserId, String name, String color) {
        Tag tag = Tag.create(currentUserId, name, color);
        requireNameAvailable(currentUserId, tag.name(), null);
        return tagRepositoryPort.save(tag);
    }

    public Tag update(Long currentUserId, Long tagId, String name, String color) {
        Tag existing = findOwnedOrThrow(currentUserId, tagId);
        Tag updated = existing.withDetails(name, color);
        requireNameAvailable(currentUserId, updated.name(), tagId);
        return tagRepositoryPort.save(updated);
    }

    /** Some das transações e recorrências que a usavam; os lançamentos continuam. */
    public void delete(Long currentUserId, Long tagId) {
        findOwnedOrThrow(currentUserId, tagId);
        tagRepositoryPort.deleteById(tagId);
    }

    /**
     * Converte nomes em tags do usuário, criando as que ainda não existem. Nomes repetidos (mesmo
     * que escritos diferente) contam uma vez só. {@code null} ou lista vazia = nenhuma tag.
     */
    @Transactional
    public List<Tag> resolveOrCreate(Long currentUserId, Collection<String> rawNames) {
        if (rawNames == null || rawNames.isEmpty()) {
            return List.of();
        }
        Set<String> names = new LinkedHashSet<>();
        for (String rawName : rawNames) {
            names.add(Tag.normalizeName(rawName));
        }
        if (names.size() > Tag.MAX_TAGS_PER_ITEM) {
            throw new InvalidTagException(
                    "Use no máximo " + Tag.MAX_TAGS_PER_ITEM + " tags por lançamento.");
        }
        Map<String, Tag> existingByName =
                tagRepositoryPort.findAllByUserIdAndNames(currentUserId, names).stream()
                        .collect(Collectors.toMap(Tag::name, Function.identity()));
        List<Tag> resolved = new ArrayList<>();
        for (String name : names) {
            Tag tag = existingByName.get(name);
            resolved.add(
                    tag != null
                            ? tag
                            : tagRepositoryPort.save(Tag.create(currentUserId, name, null)));
        }
        return resolved;
    }

    /** Troca as tags da transação (a posse da transação é conferida por quem chama). */
    @Transactional
    public List<Tag> replaceTransactionTags(
            Long currentUserId, Long transactionId, Collection<String> tagNames) {
        List<Tag> tags = resolveOrCreate(currentUserId, tagNames);
        tagRepositoryPort.replaceTransactionTags(
                transactionId, tags.stream().map(Tag::id).toList());
        return tags;
    }

    @Transactional
    public List<Tag> replaceRecurringTransactionTags(
            Long currentUserId, Long recurringTransactionId, Collection<String> tagNames) {
        List<Tag> tags = resolveOrCreate(currentUserId, tagNames);
        tagRepositoryPort.replaceRecurringTransactionTags(
                recurringTransactionId, tags.stream().map(Tag::id).toList());
        return tags;
    }

    /** Copia para uma transação as tags já resolvidas (ex.: as da recorrência que a gerou). */
    public void linkTransaction(Long transactionId, Collection<Long> tagIds) {
        if (!tagIds.isEmpty()) {
            tagRepositoryPort.replaceTransactionTags(transactionId, tagIds);
        }
    }

    /** Tags de cada transação, em ordem alfabética; transação sem tag não aparece no mapa. */
    public Map<Long, List<Tag>> tagsByTransactionIds(
            Long currentUserId, Collection<Long> transactionIds) {
        return resolveLinks(
                currentUserId, tagRepositoryPort.findTagIdsByTransactionIds(transactionIds));
    }

    public Map<Long, List<Tag>> tagsByRecurringTransactionIds(
            Long currentUserId, Collection<Long> recurringTransactionIds) {
        return resolveLinks(
                currentUserId,
                tagRepositoryPort.findTagIdsByRecurringTransactionIds(recurringTransactionIds));
    }

    public List<Long> tagIdsOfRecurringTransaction(Long recurringTransactionId) {
        return tagRepositoryPort
                .findTagIdsByRecurringTransactionIds(List.of(recurringTransactionId))
                .getOrDefault(recurringTransactionId, List.of());
    }

    /** Tags do usuário por id — para validar filtros e montar relatórios. */
    public Map<Long, Tag> tagsById(Long currentUserId) {
        return tagRepositoryPort.findAllByUserId(currentUserId).stream()
                .collect(Collectors.toMap(Tag::id, Function.identity()));
    }

    private Map<Long, List<Tag>> resolveLinks(Long currentUserId, Map<Long, List<Long>> links) {
        if (links.isEmpty()) {
            return Map.of();
        }
        Map<Long, Tag> tagById = tagsById(currentUserId);
        return links.entrySet().stream()
                .collect(
                        Collectors.toMap(
                                Map.Entry::getKey,
                                entry ->
                                        entry.getValue().stream()
                                                .map(tagById::get)
                                                .filter(Objects::nonNull)
                                                .sorted(Comparator.comparing(Tag::name))
                                                .toList()));
    }

    private void requireNameAvailable(Long currentUserId, String name, Long ignoredTagId) {
        boolean taken =
                tagRepositoryPort.findAllByUserIdAndNames(currentUserId, List.of(name)).stream()
                        .anyMatch(tag -> !tag.id().equals(ignoredTagId));
        if (taken) {
            throw new TagAlreadyExistsException("Já existe a tag #" + name + ".");
        }
    }

    private Tag findOwnedOrThrow(Long currentUserId, Long tagId) {
        return tagRepositoryPort
                .findById(tagId)
                .filter(tag -> tag.belongsTo(currentUserId))
                .orElseThrow(() -> new ResourceNotFoundException("Tag não encontrada: " + tagId));
    }
}
