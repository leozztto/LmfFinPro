package com.lmf.finpro.infrastructure.persistence.repository;

import com.lmf.finpro.infrastructure.persistence.entity.RecurringTransactionTagJpaEntity;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecurringTransactionTagJpaRepository
        extends JpaRepository<
                RecurringTransactionTagJpaEntity, RecurringTransactionTagJpaEntity.Key> {

    List<RecurringTransactionTagJpaEntity> findByRecurringTransactionIdIn(
            Collection<Long> recurringTransactionIds);

    @Modifying
    @Query(
            "DELETE FROM RecurringTransactionTagJpaEntity t"
                    + " WHERE t.recurringTransactionId = :recurringTransactionId")
    void deleteByRecurringTransactionId(
            @Param("recurringTransactionId") Long recurringTransactionId);
}
