package com.lmf.finpro.domain.model;

/** Vínculo de um usuário com o seu (único) grupo. */
public record HouseholdMembership(Long householdId, Long userId, HouseholdRole role) {

    public boolean isOwner() {
        return role == HouseholdRole.OWNER;
    }
}
