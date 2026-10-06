package com.lmf.finpro.infrastructure.web.dto.household;

import com.lmf.finpro.domain.model.HouseholdRole;
import com.lmf.finpro.domain.model.HouseholdType;

/** {@code role} é o papel de quem pediu naquele grupo. */
public record HouseholdResponse(Long id, String name, HouseholdType type, HouseholdRole role) {}
