package com.lmf.finpro.domain.model;

public record Category(
    Long id,
    Long userId,
    String name,
    CategoryType type,
    String color,
    String icon
) {

    public static Category create(Long userId, String name, CategoryType type, String color, String icon) {
        return new Category(null, userId, name, type, color, icon);
    }

    /** Categoria padrão do sistema, disponível para todos os usuários. */
    public boolean isGlobal() {
        return userId == null;
    }

    public boolean isOwnedBy(Long candidateUserId) {
        return userId != null && userId.equals(candidateUserId);
    }

    public boolean isVisibleTo(Long candidateUserId) {
        return isGlobal() || isOwnedBy(candidateUserId);
    }

    public Category withDetails(String newName, CategoryType newType, String newColor, String newIcon) {
        return new Category(id, userId, newName, newType, newColor, newIcon);
    }
}
