package com.lmf.finpro.infrastructure.web.dto.auth;

import com.lmf.finpro.application.legal.LegalDocuments;
import com.lmf.finpro.domain.model.DocumentType;
import com.lmf.finpro.domain.model.TaxRegime;
import com.lmf.finpro.infrastructure.web.validation.HasDocument;
import com.lmf.finpro.infrastructure.web.validation.HasTaxRegimeDocument;
import com.lmf.finpro.infrastructure.web.validation.ValidDocumentNumber;
import com.lmf.finpro.infrastructure.web.validation.ValidTaxRegimeDocument;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * {@code termsVersion} e {@code privacyVersion} são as versões dos documentos que a pessoa viu na
 * tela (obtidas em {@code GET /api/legal/versions}); o cadastro só vale se forem as vigentes.
 */
@ValidDocumentNumber
@ValidTaxRegimeDocument
public record RegisterRequest(
        @NotBlank(message = "nome é obrigatório")
                @Pattern(regexp = "^\\S+\\s+\\S+.*$", message = "informe nome e sobrenome")
                String name,
        @NotBlank(message = "e-mail é obrigatório") @Email(message = "e-mail inválido")
                String email,
        @NotBlank(message = "senha é obrigatória")
                @Size(min = 8, message = "senha deve ter ao menos 8 caracteres")
                String password,
        @NotNull(message = "tipo de documento é obrigatório") DocumentType documentType,
        @NotBlank(message = "documento é obrigatório") String documentNumber,
        @Pattern(regexp = "\\d{10,11}", message = "telefone deve ter 10 ou 11 dígitos (com DDD)")
                String phone,
        @NotNull(message = "regime tributário é obrigatório") TaxRegime taxRegime,
        @NotNull(message = "endereço é obrigatório") @Valid AddressRequest address,
        String inviteToken,
        @NotBlank(message = "é preciso aceitar os Termos de Uso") String termsVersion,
        @NotBlank(message = "é preciso aceitar a Política de Privacidade") String privacyVersion)
        implements HasDocument, HasTaxRegimeDocument {

    /** Cadastro comum, sem convite, aceitando as versões vigentes (atalho dos testes). */
    public RegisterRequest(
            String name,
            String email,
            String password,
            DocumentType documentType,
            String documentNumber,
            String phone,
            TaxRegime taxRegime,
            AddressRequest address) {
        this(name, email, password, documentType, documentNumber, phone, taxRegime, address, null);
    }

    /** Cadastro (com convite ou não) aceitando as versões vigentes (atalho dos testes). */
    public RegisterRequest(
            String name,
            String email,
            String password,
            DocumentType documentType,
            String documentNumber,
            String phone,
            TaxRegime taxRegime,
            AddressRequest address,
            String inviteToken) {
        this(
                name,
                email,
                password,
                documentType,
                documentNumber,
                phone,
                taxRegime,
                address,
                inviteToken,
                LegalDocuments.TERMS_VERSION,
                LegalDocuments.PRIVACY_VERSION);
    }
}
