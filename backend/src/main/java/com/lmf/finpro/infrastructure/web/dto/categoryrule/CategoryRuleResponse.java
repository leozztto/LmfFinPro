package com.lmf.finpro.infrastructure.web.dto.categoryrule;

public record CategoryRuleResponse(
    Long id,
    String pattern,
    Long categoryId,
    int weight
) {
}
