package com.lmf.finpro.application.household;

import com.lmf.finpro.domain.model.HouseholdRole;
import com.lmf.finpro.domain.model.HouseholdType;

/** Um grupo do ponto de vista de quem participa dele: o seu papel nele. */
public record HouseholdSummary(Long id, String name, HouseholdType type, HouseholdRole role) {}
