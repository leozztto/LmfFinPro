package com.lmf.finpro.infrastructure.web.dto.legal;

import jakarta.validation.constraints.NotBlank;

/** As versões que a pessoa leu na tela; só valem se ainda forem as vigentes. */
public record AcceptConsentRequest(
        @NotBlank(message = "versão dos termos é obrigatória") String termsVersion,
        @NotBlank(message = "versão da política é obrigatória") String privacyVersion) {}
