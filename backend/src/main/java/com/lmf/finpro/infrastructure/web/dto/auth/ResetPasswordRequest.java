package com.lmf.finpro.infrastructure.web.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "token é obrigatório") String token,
        @NotBlank(message = "senha é obrigatória")
                @Size(min = 8, message = "senha deve ter ao menos 8 caracteres")
                String password) {}
