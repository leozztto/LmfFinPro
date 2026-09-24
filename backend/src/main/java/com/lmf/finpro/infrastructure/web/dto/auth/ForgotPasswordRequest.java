package com.lmf.finpro.infrastructure.web.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(
        @NotBlank(message = "e-mail é obrigatório") @Email(message = "e-mail inválido")
                String email) {}
