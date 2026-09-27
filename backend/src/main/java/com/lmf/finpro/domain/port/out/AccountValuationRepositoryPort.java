package com.lmf.finpro.domain.port.out;

import com.lmf.finpro.domain.model.AccountValuation;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AccountValuationRepositoryPort {
    AccountValuation save(AccountValuation valuation);

    Optional<AccountValuation> findById(Long id);

    Optional<AccountValuation> findByAccountIdAndDate(Long accountId, LocalDate valuationDate);

    /** Mais recentes primeiro. */
    List<AccountValuation> findAllByAccountId(Long accountId);

    List<AccountValuation> findAllByAccountIds(List<Long> accountIds);

    boolean existsByAccountId(Long accountId);

    void deleteById(Long id);
}
