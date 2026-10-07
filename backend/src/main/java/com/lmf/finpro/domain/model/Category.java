package com.lmf.finpro.domain.model;

public record Category(
        Long id, Long householdId, String name, CategoryType type, String color, String icon) {

    public static Category create(
            Long householdId, String name, CategoryType type, String color, String icon) {
        return new Category(null, householdId, name, type, color, icon);
    }

    /** Categoria padrão do sistema, disponível para todos os usuários. */
    public boolean isGlobal() {
        return householdId == null;
    }

    public boolean isOwnedBy(Long candidateHouseholdId) {
        return householdId != null && householdId.equals(candidateHouseholdId);
    }

    public boolean isVisibleTo(Long candidateHouseholdId) {
        return isGlobal() || isOwnedBy(candidateHouseholdId);
    }

    public Category withDetails(
            String newName, CategoryType newType, String newColor, String newIcon) {
        return new Category(id, householdId, newName, newType, newColor, newIcon);
    }
}
