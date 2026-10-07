package com.lmf.finpro.application.auth;

import com.lmf.finpro.domain.model.DocumentType;
import com.lmf.finpro.domain.model.TaxRegime;

/**
 * @param inviteToken token de um convite para grupo compartilhado, quando o cadastro vem de um
 *     convite; nulo ou em branco para um cadastro comum
 * @param termsVersion versão dos Termos de Uso que a pessoa leu e aceitou
 * @param privacyVersion versão da Política de Privacidade que a pessoa leu e aceitou
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
        String inviteToken,
        String termsVersion,
        String privacyVersion) {}
