package com.lmf.finpro.application.auth;

import com.lmf.finpro.domain.model.DocumentType;
import com.lmf.finpro.domain.model.TaxRegime;

/**
 * @param inviteToken token de um convite para grupo compartilhado, quando o cadastro vem de um
 *     convite; nulo ou em branco para um cadastro comum
 */
public record RegisterCommand(
        String name,
        String email,
        String rawPassword,
        DocumentType documentType,
        String documentNumber,
        String phone,
        TaxRegime taxRegime,
        AddressCommand address,
        String inviteToken) {

    /** Cadastro comum, sem convite. */
    public RegisterCommand(
            String name,
            String email,
            String rawPassword,
            DocumentType documentType,
            String documentNumber,
            String phone,
            TaxRegime taxRegime,
            AddressCommand address) {
        this(
                name,
                email,
                rawPassword,
                documentType,
                documentNumber,
                phone,
                taxRegime,
                address,
                null);
    }
}
