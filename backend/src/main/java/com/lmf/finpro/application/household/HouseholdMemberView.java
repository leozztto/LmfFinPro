package com.lmf.finpro.application.household;

import com.lmf.finpro.domain.model.HouseholdRole;

public record HouseholdMemberView(Long userId, String name, String email, HouseholdRole role) {}
