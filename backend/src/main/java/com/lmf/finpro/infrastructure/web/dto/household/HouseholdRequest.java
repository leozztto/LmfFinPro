package com.lmf.finpro.infrastructure.web.dto.household;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record HouseholdRequest(
        @NotBlank(message = "nome é obrigatório")
                @Size(max = 150, message = "nome deve ter no máximo 150 caracteres")
                String name) {}
