package com.lmf.finpro.domain.port.out;

import com.lmf.finpro.domain.model.HouseholdInvite;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface HouseholdInviteRepositoryPort {
    HouseholdInvite save(HouseholdInvite invite);

    Optional<HouseholdInvite> findById(Long id);

    Optional<HouseholdInvite> findByTokenHash(String tokenHash);

    /**
     * Convites ainda válidos (não aceitos e não expirados) do grupo, do mais novo ao mais antigo.
     */
    List<HouseholdInvite> findPendingByHouseholdId(Long householdId, LocalDateTime now);

    /**
     * Convites pendentes endereçados a este e-mail (sem diferenciar maiúsculas), do mais novo ao
     * mais antigo.
     */
    List<HouseholdInvite> findPendingByEmail(String email, LocalDateTime now);

    /** Um novo convite para o mesmo e-mail substitui os anteriores ainda pendentes. */
    void deletePendingByHouseholdIdAndEmail(Long householdId, String email, LocalDateTime now);

    void deleteById(Long id);
}
