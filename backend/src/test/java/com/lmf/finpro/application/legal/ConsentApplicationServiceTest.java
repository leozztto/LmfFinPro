package com.lmf.finpro.application.legal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lmf.finpro.domain.exception.OutdatedLegalDocumentException;
import com.lmf.finpro.domain.model.LegalDocumentType;
import com.lmf.finpro.domain.model.UserConsent;
import com.lmf.finpro.domain.port.out.UserConsentRepositoryPort;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConsentApplicationServiceTest {

    private static final Long USER_ID = 7L;
    private static final LocalDateTime ACCEPTED_AT = LocalDateTime.of(2026, 10, 7, 9, 30);

    @Mock private UserConsentRepositoryPort userConsentRepositoryPort;

    @InjectMocks private ConsentApplicationService service;

    private static UserConsent consent(LegalDocumentType type, String version) {
        return new UserConsent(type, version, ACCEPTED_AT);
    }

    @Test
    void userWhoNeverAcceptedIsPending() {
        when(userConsentRepositoryPort.findByUserId(USER_ID)).thenReturn(List.of());

        ConsentStatus status = service.status(USER_ID);

        assertThat(status.pending()).isTrue();
        assertThat(status.terms().accepted()).isFalse();
        assertThat(status.terms().acceptedVersion()).isNull();
        assertThat(status.terms().currentVersion()).isEqualTo(LegalDocuments.TERMS_VERSION);
    }

    @Test
    void userWhoAcceptedTheCurrentVersionsIsNotPending() {
        when(userConsentRepositoryPort.findByUserId(USER_ID))
                .thenReturn(
                        List.of(
                                consent(LegalDocumentType.TERMS, LegalDocuments.TERMS_VERSION),
                                consent(
                                        LegalDocumentType.PRIVACY,
                                        LegalDocuments.PRIVACY_VERSION)));

        ConsentStatus status = service.status(USER_ID);

        assertThat(status.pending()).isFalse();
        assertThat(status.terms().acceptedAt()).isEqualTo(ACCEPTED_AT);
    }

    @Test
    void acceptingOnlyAnOldVersionStaysPending() {
        when(userConsentRepositoryPort.findByUserId(USER_ID))
                .thenReturn(
                        List.of(
                                consent(LegalDocumentType.TERMS, "2020-01-01"),
                                consent(
                                        LegalDocumentType.PRIVACY,
                                        LegalDocuments.PRIVACY_VERSION)));

        ConsentStatus status = service.status(USER_ID);

        assertThat(status.pending()).isTrue();
        assertThat(status.terms().accepted()).isFalse();
        assertThat(status.terms().acceptedVersion()).isEqualTo("2020-01-01");
        assertThat(status.privacy().accepted()).isTrue();
    }

    @Test
    void acceptRecordsBothDocumentsAtTheirCurrentVersions() {
        when(userConsentRepositoryPort.findByUserId(USER_ID)).thenReturn(List.of());

        service.accept(USER_ID, LegalDocuments.TERMS_VERSION, LegalDocuments.PRIVACY_VERSION);

        verify(userConsentRepositoryPort)
                .save(USER_ID, LegalDocumentType.TERMS, LegalDocuments.TERMS_VERSION);
        verify(userConsentRepositoryPort)
                .save(USER_ID, LegalDocumentType.PRIVACY, LegalDocuments.PRIVACY_VERSION);
    }

    @Test
    void acceptRefusesAnOutdatedTermsVersionAndSavesNothing() {
        assertThatThrownBy(
                        () -> service.accept(USER_ID, "2020-01-01", LegalDocuments.PRIVACY_VERSION))
                .isInstanceOf(OutdatedLegalDocumentException.class);

        verify(userConsentRepositoryPort, never())
                .save(USER_ID, LegalDocumentType.TERMS, "2020-01-01");
        verify(userConsentRepositoryPort, never())
                .save(USER_ID, LegalDocumentType.PRIVACY, LegalDocuments.PRIVACY_VERSION);
    }

    @Test
    void requireCurrentRefusesNullVersions() {
        assertThatThrownBy(() -> service.requireCurrent(null, null))
                .isInstanceOf(OutdatedLegalDocumentException.class);
    }
}
