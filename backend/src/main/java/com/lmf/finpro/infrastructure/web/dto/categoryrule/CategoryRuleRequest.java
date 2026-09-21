package com.lmf.finpro.infrastructure.web.dto.categoryrule;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CategoryRuleRequest(
        @NotBlank(message = "padrão é obrigatório") String pattern,
        @NotNull(message = "categoria é obrigatória") Long categoryId) {}
