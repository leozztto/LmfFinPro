package com.lmf.finpro.application.support;

import com.lmf.finpro.domain.model.HouseholdType;
import com.lmf.finpro.domain.model.TaxRegime;
import com.lmf.finpro.domain.model.User;
import com.lmf.finpro.domain.port.out.HouseholdRepositoryPort;
import com.lmf.finpro.domain.port.out.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regime tributário (MEI, Simples, autônomo...) é uma característica da <b>pessoa</b>, não do
 * dinheiro: só o espaço pessoal tem um, o do seu único membro. Um grupo compartilhado (casal,
 * família) junta pessoas com regimes diferentes e não tem regime próprio, então recursos que
 * dependem dele (lembrete do DAS, pró-labore, alíquota sugerida das metas) ficam só no espaço
 * pessoal.
 */
@Service
@RequiredArgsConstructor
public class HouseholdTaxProfile {

    private final HouseholdRepositoryPort householdRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;

    /** Nulo para grupo compartilhado ou quando a pessoa não informou regime. */
    @Transactional(readOnly = true)
    public TaxRegime regimeOf(Long householdId) {
        return householdRepositoryPort
                .findById(householdId)
                .filter(household -> household.type() == HouseholdType.PERSONAL)
                .flatMap(
                        household ->
                                householdRepositoryPort
                                        .findMembershipsByHouseholdId(household.id())
                                        .stream()
                                        .findFirst())
                .flatMap(membership -> userRepositoryPort.findById(membership.userId()))
                .map(User::taxRegime)
                .orElse(null);
    }
}
