package com.lmf.finpro.infrastructure.web.dto.legal;

import com.lmf.finpro.application.legal.ConsentStatus;
import java.time.LocalDateTime;

/** {@code pending} é verdadeiro enquanto houver documento com versão vigente ainda não aceita. */
public record ConsentStatusResponse(
        DocumentResponse terms, DocumentResponse privacy, boolean pending) {

    public record DocumentResponse(
            String currentVersion,
            String acceptedVersion,
            LocalDateTime acceptedAt,
            boolean accepted) {

        static DocumentResponse from(ConsentStatus.DocumentStatus status) {
            return new DocumentResponse(
                    status.currentVersion(),
                    status.acceptedVersion(),
                    status.acceptedAt(),
                    status.accepted());
        }
    }

    public static ConsentStatusResponse from(ConsentStatus status) {
        return new ConsentStatusResponse(
                DocumentResponse.from(status.terms()),
                DocumentResponse.from(status.privacy()),
                status.pending());
    }
}
