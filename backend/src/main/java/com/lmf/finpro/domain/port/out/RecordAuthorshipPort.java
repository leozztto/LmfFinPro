package com.lmf.finpro.domain.port.out;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * Quem criou cada lançamento e cada transferência. Fica à parte dos modelos de domínio de
 * propósito: a autoria só importa para a regra de exclusão e para mostrar "lançado por", e assim
 * não toca em todas as construções de {@code Transaction} e {@code Transfer}.
 */
public interface RecordAuthorshipPort {

    void recordTransactionAuthors(Collection<Long> transactionIds, Long userId);

    void recordTransferAuthor(Long transferId, Long userId);

    /** Vazio quando o autor é desconhecido (ou o registro não existe). */
    Optional<Long> findTransactionAuthor(Long transactionId);

    Optional<Long> findTransferAuthor(Long transferId);

    /** Só entram no mapa os registros com autor conhecido. */
    Map<Long, Long> findTransactionAuthors(Collection<Long> transactionIds);

    Map<Long, Long> findTransferAuthors(Collection<Long> transferIds);
}
