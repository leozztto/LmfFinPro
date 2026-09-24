package com.lmf.finpro.infrastructure.web.dto.profile;

import com.lmf.finpro.domain.model.DocumentType;
import com.lmf.finpro.domain.model.TaxRegime;
import com.lmf.finpro.infrastructure.web.dto.auth.AddressRequest;
import com.lmf.finpro.infrastructure.web.validation.HasDocument;
import com.lmf.finpro.infrastructure.web.validation.HasTaxRegimeDocument;
import com.lmf.finpro.infrastructure.web.validation.ValidDocumentNumber;
import com.lmf.finpro.infrastructure.web.validation.ValidTaxRegimeDocument;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** Mesmas regras do cadastro (RegisterRequest), sem a senha nova. */
@ValidDocumentNumber
@ValidTaxRegimeDocument
public record UpdateProfileRequest(
        @NotBlank(message = "nome é obrigatório")
                @Pattern(regexp = "^\\S+\\s+\\S+.*$", message = "informe nome e sobrenome")
                String name,
        @NotBlank(message = "e-mail é obrigatório") @Email(message = "e-mail inválido")
                String email,
        @NotNull(message = "tipo de documento é obrigatório") DocumentType documentType,
        @NotBlank(message = "documento é obrigatório") String documentNumber,
        @Pattern(regexp = "\\d{10,11}", message = "telefone deve ter 10 ou 11 dígitos (com DDD)")
                String phone,
        @NotNull(message = "regime tributário é obrigatório") TaxRegime taxRegime,
        @NotNull(message = "endereço é obrigatório") @Valid AddressRequest address,
        /* Só exigida quando o e-mail muda — validado no ProfileApplicationService. */
        String currentPassword)
        implements HasDocument, HasTaxRegimeDocument {}
