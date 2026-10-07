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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tags do usuário e os vínculos delas com transações e recorrências. As telas mandam os
 * <b>nomes</b> das tags; aqui eles são normalizados e cada um vira a tag existente ou uma nova —
 * assim "#Site Acme" e "site-acme" sempre caem na mesma tag.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TagApplicationService {

    private final TagRepositoryPort tagRepositoryPort;

    /** Uma tag com quantas transações a usam (para a tela de gestão). */
    public record TagUsage(Tag tag, long transactionCount) {}

    public List<TagUsage> list(Long currentHouseholdId) {
        log.debug("Listando tags do usuário={}", currentHouseholdId);
        List<Tag> tags = tagRepositoryPort.findAllByHouseholdId(currentHouseholdId);
        Map<Long, Long> counts =
                tagRepositoryPort.countTransactionsByTagIds(tags.stream().map(Tag::id).toList());
        return tags.stream()
                .map(tag -> new TagUsage(tag, counts.getOrDefault(tag.id(), 0L)))
                .toList();
    }

    public Tag create(Long currentHouseholdId, String name, String color) {
        log.debug("Criando tag para o usuário={}", currentHouseholdId);
        Tag tag = Tag.create(currentHouseholdId, name, color);
        requireNameAvailable(currentHouseholdId, tag.name(), null);
        Tag saved = tagRepositoryPort.save(tag);
        log.debug("Tag={} criada para o usuário={}", saved.id(), currentHouseholdId);
        return saved;
    }

    public Tag update(Long currentHouseholdId, Long tagId, String name, String color) {
        log.debug("Atualizando tag={} do usuário={}", tagId, currentHouseholdId);
        Tag existing = findOwnedOrThrow(currentHouseholdId, tagId);
        Tag updated = existing.withDetails(name, color);
        requireNameAvailable(currentHouseholdId, updated.name(), tagId);
        return tagRepositoryPort.save(updated);
    }

    /** Some das transações e recorrências que a usavam; os lançamentos continuam. */
    public void delete(Long currentHouseholdId, Long tagId) {
        log.debug("Removendo tag={} do usuário={}", tagId, currentHouseholdId);
        findOwnedOrThrow(currentHouseholdId, tagId);
        tagRepositoryPort.deleteById(tagId);
    }

    /**
     * Converte nomes em tags do usuário, criando as que ainda não existem. Nomes repetidos (mesmo
     * que escritos diferente) contam uma vez só. {@code null} ou lista vazia = nenhuma tag.
     */
    @Transactional
    public List<Tag> resolveOrCreate(Long currentHouseholdId, Collection<String> rawNames) {
        if (rawNames == null || rawNames.isEmpty()) {
            return List.of();
        }
        Set<String> names = new LinkedHashSet<>();
        for (String rawName : rawNames) {
            names.add(Tag.normalizeName(rawName));
        }
        log.debug("Resolvendo {} tag(s) do usuário={}", names.size(), currentHouseholdId);
        if (names.size() > Tag.MAX_TAGS_PER_ITEM) {
            throw new InvalidTagException(
                    "Use no máximo " + Tag.MAX_TAGS_PER_ITEM + " tags por lançamento.");
        }
        Map<String, Tag> existingByName =
                tagRepositoryPort.findAllByHouseholdIdAndNames(currentHouseholdId, names).stream()
                        .collect(Collectors.toMap(Tag::name, Function.identity()));
        List<Tag> resolved = new ArrayList<>();
        for (String name : names) {
            Tag tag = existingByName.get(name);
            resolved.add(
                    tag != null
                            ? tag
                            : tagRepositoryPort.save(Tag.create(currentHouseholdId, name, null)));
        }
        return resolved;
    }

    /** Troca as tags da transação (a posse da transação é conferida por quem chama). */
    @Transactional
    public List<Tag> replaceTransactionTags(
            Long currentHouseholdId, Long transactionId, Collection<String> tagNames) {
        List<Tag> tags = resolveOrCreate(currentHouseholdId, tagNames);
        tagRepositoryPort.replaceTransactionTags(
                transactionId, tags.stream().map(Tag::id).toList());
        return tags;
    }

    @Transactional
    public List<Tag> replaceRecurringTransactionTags(
            Long currentHouseholdId, Long recurringTransactionId, Collection<String> tagNames) {
        List<Tag> tags = resolveOrCreate(currentHouseholdId, tagNames);
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
            Long currentHouseholdId, Collection<Long> transactionIds) {
        return resolveLinks(
                currentHouseholdId, tagRepositoryPort.findTagIdsByTransactionIds(transactionIds));
    }

    public Map<Long, List<Tag>> tagsByRecurringTransactionIds(
            Long currentHouseholdId, Collection<Long> recurringTransactionIds) {
        return resolveLinks(
                currentHouseholdId,
                tagRepositoryPort.findTagIdsByRecurringTransactionIds(recurringTransactionIds));
    }

    public List<Long> tagIdsOfRecurringTransaction(Long recurringTransactionId) {
        return tagRepositoryPort
                .findTagIdsByRecurringTransactionIds(List.of(recurringTransactionId))
                .getOrDefault(recurringTransactionId, List.of());
    }

    /** Tags do usuário por id — para validar filtros e montar relatórios. */
    public Map<Long, Tag> tagsById(Long currentHouseholdId) {
        return tagRepositoryPort.findAllByHouseholdId(currentHouseholdId).stream()
                .collect(Collectors.toMap(Tag::id, Function.identity()));
    }

    private Map<Long, List<Tag>> resolveLinks(
            Long currentHouseholdId, Map<Long, List<Long>> links) {
        if (links.isEmpty()) {
            return Map.of();
        }
        Map<Long, Tag> tagById = tagsById(currentHouseholdId);
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

    private void requireNameAvailable(Long currentHouseholdId, String name, Long ignoredTagId) {
        boolean taken =
                tagRepositoryPort
                        .findAllByHouseholdIdAndNames(currentHouseholdId, List.of(name))
                        .stream()
                        .anyMatch(tag -> !tag.id().equals(ignoredTagId));
        if (taken) {
            throw new TagAlreadyExistsException("Já existe a tag #" + name + ".");
        }
    }

    private Tag findOwnedOrThrow(Long currentHouseholdId, Long tagId) {
        return tagRepositoryPort
                .findById(tagId)
                .filter(tag -> tag.belongsTo(currentHouseholdId))
                .orElseThrow(() -> new ResourceNotFoundException("Tag não encontrada: " + tagId));
    }
}
