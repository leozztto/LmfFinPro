package com.lmf.finpro.infrastructure.web.dto.household;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record InviteRequest(
        @NotBlank(message = "e-mail é obrigatório") @Email(message = "e-mail inválido")
                String email) {}
