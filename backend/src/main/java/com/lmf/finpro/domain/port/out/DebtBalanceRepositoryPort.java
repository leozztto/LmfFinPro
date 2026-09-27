package com.lmf.finpro.domain.port.out;

import com.lmf.finpro.domain.model.DebtBalance;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DebtBalanceRepositoryPort {
    DebtBalance save(DebtBalance balance);

    Optional<DebtBalance> findById(Long id);

    Optional<DebtBalance> findByDebtIdAndDate(Long debtId, LocalDate balanceDate);

    /** Mais recentes primeiro. */
    List<DebtBalance> findAllByDebtId(Long debtId);

    List<DebtBalance> findAllByDebtIds(List<Long> debtIds);

    long countByDebtId(Long debtId);

    void deleteById(Long id);
}
