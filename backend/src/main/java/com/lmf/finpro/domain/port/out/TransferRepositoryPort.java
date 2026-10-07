package com.lmf.finpro.domain.port.out;

import com.lmf.finpro.domain.model.Transfer;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface TransferRepositoryPort {
    Transfer save(Transfer transfer);

    Optional<Transfer> findById(Long id);

    List<Transfer> findAllByHouseholdId(Long householdId);

    boolean existsByAccountId(Long accountId);

    /**
     * Dentre os ids, as transferências cujas duas contas estão em espaços diferentes (pessoal x
     * grupo): para quem consulta, o dinheiro entrou ou saiu de verdade.
     */
    Set<Long> findCrossSpaceIds(Collection<Long> transferIds);

    void deleteById(Long id);
}
