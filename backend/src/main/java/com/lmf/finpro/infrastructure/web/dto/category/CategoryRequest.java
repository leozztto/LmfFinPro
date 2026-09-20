package com.lmf.finpro.infrastructure.web.dto.category;

import com.lmf.finpro.domain.model.CategoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CategoryRequest(
        @NotBlank(message = "nome é obrigatório") String name,
        @NotNull(message = "tipo é obrigatório") CategoryType type,
        String color,
        String icon) {}
