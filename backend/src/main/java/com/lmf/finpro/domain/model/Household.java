package com.lmf.finpro.domain.model;

import java.time.LocalDateTime;

/** Dono dos dados financeiros: o espaço pessoal de uma pessoa ou um grupo compartilhado. */
public record Household(Long id, String name, HouseholdType type, LocalDateTime createdAt) {

    public static Household personal(String name) {
        return new Household(null, name, HouseholdType.PERSONAL, LocalDateTime.now());
    }

    public static Household shared(String name) {
        return new Household(null, name, HouseholdType.SHARED, LocalDateTime.now());
    }

    public boolean isShared() {
        return type == HouseholdType.SHARED;
    }
}
