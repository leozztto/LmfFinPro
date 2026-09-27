package com.lmf.finpro.domain.port.out;

import com.lmf.finpro.domain.model.Tag;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface TagRepositoryPort {
    Tag save(Tag tag);

    Optional<Tag> findById(Long id);

    List<Tag> findAllByUserId(Long userId);

    /** Tags do usuário com esses nomes (já normalizados). */
    List<Tag> findAllByUserIdAndNames(Long userId, Collection<String> names);

    void deleteById(Long id);

    // --- vínculos com transações e recorrências ---

    /** Ids das tags de cada transação; transação sem tag não aparece no mapa. */
    Map<Long, List<Long>> findTagIdsByTransactionIds(Collection<Long> transactionIds);

    /** Troca todas as tags da transação por estas (lista vazia = sem tags). */
    void replaceTransactionTags(Long transactionId, Collection<Long> tagIds);

    /** Quantas transações usam cada tag; tag sem uso não aparece no mapa. */
    Map<Long, Long> countTransactionsByTagIds(Collection<Long> tagIds);

    Map<Long, List<Long>> findTagIdsByRecurringTransactionIds(
            Collection<Long> recurringTransactionIds);

    void replaceRecurringTransactionTags(Long recurringTransactionId, Collection<Long> tagIds);
}
