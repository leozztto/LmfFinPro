package com.lmf.finpro.infrastructure.web.dto.auth;

import com.lmf.finpro.domain.model.DocumentType;
import com.lmf.finpro.infrastructure.web.validation.HasDocument;
import com.lmf.finpro.infrastructure.web.validation.ValidDocumentNumber;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@ValidDocumentNumber
public record RegisterRequest(
    @NotBlank(message = "nome é obrigatório")
    @Pattern(regexp = "^\\S+\\s+\\S+.*$", message = "informe nome e sobrenome")
    String name,

    @NotBlank(message = "e-mail é obrigatório")
    @Email(message = "e-mail inválido")
    String email,

    @NotBlank(message = "senha é obrigatória")
    @Size(min = 8, message = "senha deve ter ao menos 8 caracteres")
    String password,

    @NotNull(message = "tipo de documento é obrigatório")
    DocumentType documentType,

    @NotBlank(message = "documento é obrigatório")
    String documentNumber,

    @Pattern(regexp = "\\d{10,11}", message = "telefone deve ter 10 ou 11 dígitos (com DDD)")
    String phone,

    String taxRegime,

    @NotNull(message = "endereço é obrigatório")
    @Valid
    AddressRequest address
) implements HasDocument {
}
