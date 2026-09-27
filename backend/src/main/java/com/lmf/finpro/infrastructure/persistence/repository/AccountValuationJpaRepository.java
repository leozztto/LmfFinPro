package com.lmf.finpro.infrastructure.persistence.repository;

import com.lmf.finpro.infrastructure.persistence.entity.AccountValuationJpaEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountValuationJpaRepository
        extends JpaRepository<AccountValuationJpaEntity, Long> {
    Optional<AccountValuationJpaEntity> findByAccountIdAndValuationDate(
            Long accountId, LocalDate valuationDate);

    List<AccountValuationJpaEntity> findByAccountIdOrderByValuationDateDesc(Long accountId);

    List<AccountValuationJpaEntity> findByAccountIdIn(List<Long> accountIds);

    boolean existsByAccountId(Long accountId);
}
