package com.lmf.finpro.domain.port.out;

import com.lmf.finpro.domain.model.Household;
import com.lmf.finpro.domain.model.HouseholdMembership;
import java.util.List;
import java.util.Optional;

public interface HouseholdRepositoryPort {
    Household save(Household household);

    Optional<Household> findById(Long id);

    HouseholdMembership saveMembership(HouseholdMembership membership);

    void deleteMembership(Long householdId, Long userId);

    Optional<HouseholdMembership> findMembership(Long householdId, Long userId);

    /** O espaço pessoal do usuário: o grupo ativo quando a requisição não escolhe outro. */
    Optional<HouseholdMembership> findPersonalMembership(Long userId);

    List<HouseholdMembership> findMembershipsByUserId(Long userId);

    List<HouseholdMembership> findMembershipsByHouseholdId(Long householdId);
}
