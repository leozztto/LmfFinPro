package com.lmf.finpro.infrastructure.persistence.repository;

import com.lmf.finpro.infrastructure.persistence.entity.TransferJpaEntity;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransferJpaRepository extends JpaRepository<TransferJpaEntity, Long> {
    List<TransferJpaEntity> findByHouseholdId(Long householdId);

    boolean existsByFromAccountIdOrToAccountId(Long fromAccountId, Long toAccountId);

    @Query(
            """
            SELECT t.id FROM TransferJpaEntity t
            WHERE t.id IN :ids AND t.fromAccount.householdId <> t.toAccount.householdId
            """)
    List<Long> findCrossSpaceIds(@Param("ids") Collection<Long> ids);
}
