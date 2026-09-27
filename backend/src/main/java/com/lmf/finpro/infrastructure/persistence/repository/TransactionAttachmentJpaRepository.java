package com.lmf.finpro.infrastructure.persistence.repository;

import com.lmf.finpro.infrastructure.persistence.entity.TransactionAttachmentJpaEntity;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionAttachmentJpaRepository
        extends JpaRepository<TransactionAttachmentJpaEntity, Long> {

    List<TransactionAttachmentJpaEntity> findByTransactionIdOrderByCreatedAtAscIdAsc(
            Long transactionId);

    List<TransactionAttachmentJpaEntity> findByTransactionIdIn(Collection<Long> transactionIds);

    /** Linhas {transactionId, quantidade}. */
    @Query(
            """
        SELECT a.transactionId, COUNT(a) FROM TransactionAttachmentJpaEntity a
        WHERE a.transactionId IN :transactionIds GROUP BY a.transactionId
        """)
    List<Object[]> countGroupedByTransactionId(
            @Param("transactionIds") Collection<Long> transactionIds);
}
