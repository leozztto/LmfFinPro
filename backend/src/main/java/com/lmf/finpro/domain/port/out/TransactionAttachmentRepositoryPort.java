package com.lmf.finpro.domain.port.out;

import com.lmf.finpro.domain.model.TransactionAttachment;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface TransactionAttachmentRepositoryPort {
    TransactionAttachment save(TransactionAttachment attachment);

    Optional<TransactionAttachment> findById(Long id);

    /** Mais antigos primeiro. */
    List<TransactionAttachment> findAllByTransactionId(Long transactionId);

    List<TransactionAttachment> findAllByTransactionIds(Collection<Long> transactionIds);

    /** Quantidade de anexos por transação (transações sem anexo ficam de fora do mapa). */
    Map<Long, Long> countByTransactionIds(Collection<Long> transactionIds);

    void deleteById(Long id);
}
