package com.lmf.finpro.infrastructure.web.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank(message = "nome é obrigatório") String name,
    @NotBlank(message = "e-mail é obrigatório") @Email(message = "e-mail inválido") String email,
    @NotBlank(message = "senha é obrigatória") @Size(min = 8, message = "senha deve ter ao menos 8 caracteres") String password,
    String taxRegime
) {
}
