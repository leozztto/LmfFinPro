package com.lmf.finpro.infrastructure.web.dto.client;

import com.lmf.finpro.domain.model.ClientWorkType;
import com.lmf.finpro.domain.model.DocumentType;
import com.lmf.finpro.infrastructure.web.validation.HasDocument;
import com.lmf.finpro.infrastructure.web.validation.ValidDocumentNumber;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@ValidDocumentNumber
public record ClientRequest(
        @NotBlank(message = "nome é obrigatório") String name,
        @NotBlank(message = "e-mail é obrigatório") @Email(message = "e-mail inválido")
                String email,
        @NotBlank(message = "telefone é obrigatório")
                @Pattern(
                        regexp = "\\d{10,11}",
                        message = "telefone deve ter 10 ou 11 dígitos (com DDD)")
                String phone,
        @NotNull(message = "tipo de documento é obrigatório") DocumentType documentType,
        @NotBlank(message = "documento é obrigatório") String documentNumber,
        @NotNull(message = "tipo de trabalho é obrigatório") ClientWorkType workType,
        @Size(max = 1000, message = "observações devem ter no máximo 1000 caracteres") String notes,
        String color,
        boolean active)
        implements HasDocument {}
