package com.lmf.finpro.domain.model;

import java.time.LocalDateTime;

/** Aceite de uma versão de um documento legal por um usuário. */
public record UserConsent(
        LegalDocumentType documentType, String version, LocalDateTime acceptedAt) {}
