package com.lmf.finpro.infrastructure.persistence.repository;

import com.lmf.finpro.infrastructure.persistence.entity.DebtBalanceJpaEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DebtBalanceJpaRepository extends JpaRepository<DebtBalanceJpaEntity, Long> {
    Optional<DebtBalanceJpaEntity> findByDebtIdAndBalanceDate(Long debtId, LocalDate balanceDate);

    List<DebtBalanceJpaEntity> findByDebtIdOrderByBalanceDateDesc(Long debtId);

    List<DebtBalanceJpaEntity> findByDebtIdIn(List<Long> debtIds);

    long countByDebtId(Long debtId);
}
