package com.lmf.finpro.infrastructure.web.dto.legal;

/** Versões vigentes dos documentos legais, as mesmas que o cadastro e o aceite exigem. */
public record LegalVersionsResponse(String termsVersion, String privacyVersion) {}
