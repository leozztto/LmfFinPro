package com.lmf.finpro.infrastructure.persistence.repository;

import com.lmf.finpro.infrastructure.persistence.entity.TransactionTagJpaEntity;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionTagJpaRepository
        extends JpaRepository<TransactionTagJpaEntity, TransactionTagJpaEntity.Key> {

    List<TransactionTagJpaEntity> findByTransactionIdIn(Collection<Long> transactionIds);

    @Modifying
    @Query("DELETE FROM TransactionTagJpaEntity t WHERE t.transactionId = :transactionId")
    void deleteByTransactionId(@Param("transactionId") Long transactionId);

    /** Linhas {tagId, quantidade de transações}. */
    @Query(
            """
        SELECT t.tagId, COUNT(t) FROM TransactionTagJpaEntity t
        WHERE t.tagId IN :tagIds GROUP BY t.tagId
        """)
    List<Object[]> countGroupedByTagId(@Param("tagIds") Collection<Long> tagIds);
}
