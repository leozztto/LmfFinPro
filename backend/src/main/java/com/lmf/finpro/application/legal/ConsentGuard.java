package com.lmf.finpro.application.legal;

import com.lmf.finpro.domain.model.LegalDocumentType;
import com.lmf.finpro.domain.model.UserConsent;
import com.lmf.finpro.domain.port.out.UserConsentRepositoryPort;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Responde se a pessoa aceitou as versões vigentes dos documentos. Consultado a cada requisição
 * pelo filtro de segurança; por isso não se chama {@code *ApplicationService}, que o aspecto de log
 * de fluxo registraria em todas as chamadas.
 */
@Component
@RequiredArgsConstructor
public class ConsentGuard {

    private final UserConsentRepositoryPort userConsentRepositoryPort;

    public boolean hasAcceptedCurrentVersions(Long userId) {
        List<UserConsent> consents = userConsentRepositoryPort.findByUserId(userId);
        return accepted(consents, LegalDocumentType.TERMS, LegalDocuments.TERMS_VERSION)
                && accepted(consents, LegalDocumentType.PRIVACY, LegalDocuments.PRIVACY_VERSION);
    }

    private static boolean accepted(
            List<UserConsent> consents, LegalDocumentType type, String currentVersion) {
        return consents.stream()
                .anyMatch(c -> c.documentType() == type && currentVersion.equals(c.version()));
    }
}
