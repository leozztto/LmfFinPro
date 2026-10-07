package com.lmf.finpro.application.legal;

import com.lmf.finpro.application.FlowLog;
import com.lmf.finpro.domain.exception.OutdatedLegalDocumentException;
import com.lmf.finpro.domain.model.LegalDocumentType;
import com.lmf.finpro.domain.model.UserConsent;
import com.lmf.finpro.domain.port.out.UserConsentRepositoryPort;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Aceite dos termos de uso e da política de privacidade (LGPD): prova de data e versão. */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConsentApplicationService {

    private final UserConsentRepositoryPort userConsentRepositoryPort;

    public ConsentStatus status(Long userId) {
        log.debug("Consultando aceite dos documentos legais do usuário={}", userId);
        List<UserConsent> consents = userConsentRepositoryPort.findByUserId(userId);
        ConsentStatus.DocumentStatus terms =
                documentStatus(consents, LegalDocumentType.TERMS, LegalDocuments.TERMS_VERSION);
        ConsentStatus.DocumentStatus privacy =
                documentStatus(consents, LegalDocumentType.PRIVACY, LegalDocuments.PRIVACY_VERSION);
        return new ConsentStatus(terms, privacy, !terms.accepted() || !privacy.accepted());
    }

    /**
     * Registra o aceite das duas versões que o usuário viu. Recusa versão que não é a vigente: o
     * texto mudou depois que a página dele carregou, e ele precisa ler o novo antes de aceitar.
     */
    @Transactional
    public ConsentStatus accept(Long userId, String termsVersion, String privacyVersion) {
        log.debug("Registrando aceite dos documentos legais do usuário={}", userId);
        recordAcceptance(userId, termsVersion, privacyVersion);
        return status(userId);
    }

    /** Usado também pelo cadastro, na mesma transação que cria o usuário. */
    @Transactional
    public void recordAcceptance(Long userId, String termsVersion, String privacyVersion) {
        requireCurrent(termsVersion, privacyVersion);
        userConsentRepositoryPort.save(userId, LegalDocumentType.TERMS, termsVersion);
        userConsentRepositoryPort.save(userId, LegalDocumentType.PRIVACY, privacyVersion);
        FlowLog.detail("termsVersion", termsVersion);
        FlowLog.detail("privacyVersion", privacyVersion);
    }

    /** Valida as versões sem gravar nada (o cadastro valida antes de criar o usuário). */
    public void requireCurrent(String termsVersion, String privacyVersion) {
        if (!LegalDocuments.TERMS_VERSION.equals(termsVersion)
                || !LegalDocuments.PRIVACY_VERSION.equals(privacyVersion)) {
            FlowLog.detail("reason", "outdatedLegalDocuments");
            throw new OutdatedLegalDocumentException(
                    "Os Termos de Uso ou a Política de Privacidade foram atualizados. Recarregue a"
                            + " página e leia a versão atual.");
        }
    }

    private ConsentStatus.DocumentStatus documentStatus(
            List<UserConsent> consents, LegalDocumentType type, String currentVersion) {
        List<UserConsent> ofType = consents.stream().filter(c -> c.documentType() == type).toList();
        UserConsent latest =
                ofType.stream().max(Comparator.comparing(UserConsent::acceptedAt)).orElse(null);
        boolean accepted = ofType.stream().anyMatch(c -> currentVersion.equals(c.version()));
        return new ConsentStatus.DocumentStatus(
                currentVersion,
                latest == null ? null : latest.version(),
                latest == null ? null : latest.acceptedAt(),
                accepted);
    }
}
