package com.lmf.finpro.domain.model;

/**
 * Regra do motor de categorização automática: quando o descritivo de uma transação importada contém
 * {@code pattern} (case-insensitive), ela é sugerida para {@code categoryId}. O peso é reforçado
 * (crescente) toda vez que o usuário confirma essa categoria para o mesmo padrão, e reiniciado
 * quando ele corrige para uma categoria diferente — assim a regra "aprende" com o uso. {@code
 * householdId} nulo é regra padrão do sistema (ex.: "UBER" → Transporte), disponível para todos os
 * usuários e somente leitura via API — só a correção do usuário cria/reforça uma regra própria.
 */
public record CategoryRule(Long id, Long householdId, String pattern, Long categoryId, int weight) {

    public static CategoryRule create(Long householdId, String pattern, Long categoryId) {
        return new CategoryRule(null, householdId, pattern, categoryId, 1);
    }

    public boolean isGlobal() {
        return householdId == null;
    }

    public boolean belongsTo(Long candidateHouseholdId) {
        return householdId != null && householdId.equals(candidateHouseholdId);
    }

    public boolean matches(String description) {
        return description != null && description.toUpperCase().contains(pattern.toUpperCase());
    }

    public CategoryRule reinforcedWith(Long confirmedCategoryId) {
        int newWeight = confirmedCategoryId.equals(categoryId) ? weight + 1 : 1;
        return new CategoryRule(id, householdId, pattern, confirmedCategoryId, newWeight);
    }

    public CategoryRule withDetails(String newPattern, Long newCategoryId) {
        return new CategoryRule(id, householdId, newPattern, newCategoryId, weight);
    }
}
