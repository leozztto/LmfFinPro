package com.lmf.finpro.domain.model;

/**
 * Regra do motor de categorização automática: quando o descritivo de uma transação importada contém
 * {@code pattern} (case-insensitive), ela é sugerida para {@code categoryId}. O peso é reforçado
 * (crescente) toda vez que o usuário confirma essa categoria para o mesmo padrão, e reiniciado
 * quando ele corrige para uma categoria diferente — assim a regra "aprende" com o uso. {@code
 * userId} nulo é regra padrão do sistema (ex.: "UBER" → Transporte), disponível para todos os
 * usuários e somente leitura via API — só a correção do usuário cria/reforça uma regra própria.
 */
public record CategoryRule(Long id, Long userId, String pattern, Long categoryId, int weight) {

    public static CategoryRule create(Long userId, String pattern, Long categoryId) {
        return new CategoryRule(null, userId, pattern, categoryId, 1);
    }

    public boolean isGlobal() {
        return userId == null;
    }

    public boolean belongsTo(Long candidateUserId) {
        return userId != null && userId.equals(candidateUserId);
    }

    public boolean matches(String description) {
        return description != null && description.toUpperCase().contains(pattern.toUpperCase());
    }

    public CategoryRule reinforcedWith(Long confirmedCategoryId) {
        int newWeight = confirmedCategoryId.equals(categoryId) ? weight + 1 : 1;
        return new CategoryRule(id, userId, pattern, confirmedCategoryId, newWeight);
    }

    public CategoryRule withDetails(String newPattern, Long newCategoryId) {
        return new CategoryRule(id, userId, newPattern, newCategoryId, weight);
    }
}
