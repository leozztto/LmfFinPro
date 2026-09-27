package com.lmf.finpro.domain.port.out;

import com.lmf.finpro.domain.model.Debt;
import java.util.List;
import java.util.Optional;

public interface DebtRepositoryPort {
    Debt save(Debt debt);

    Optional<Debt> findById(Long id);

    List<Debt> findAllByUserId(Long userId);

    void deleteById(Long id);
}
