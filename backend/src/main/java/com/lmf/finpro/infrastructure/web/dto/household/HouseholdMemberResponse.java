package com.lmf.finpro.infrastructure.web.dto.household;

import com.lmf.finpro.domain.model.HouseholdRole;

public record HouseholdMemberResponse(Long userId, String name, String email, HouseholdRole role) {}
