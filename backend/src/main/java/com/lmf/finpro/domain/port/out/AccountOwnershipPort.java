package com.lmf.finpro.domain.port.out;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * Quem é o dono de cada conta: quem a criou ou a trouxe para um grupo. Só o dono descompartilha.
 * Fica à parte do modelo {@code Account}, como a autoria dos lançamentos, para não tocar em todas
 * as construções da conta.
 */
public interface AccountOwnershipPort {

    void recordOwner(Collection<Long> accountIds, Long userId);

    /** Vazio quando o dono é desconhecido (ou a conta não existe). */
    Optional<Long> findOwner(Long accountId);

    /** Só entram no mapa as contas com dono conhecido. */
    Map<Long, Long> findOwners(Collection<Long> accountIds);
}
