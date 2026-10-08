package com.lmf.finpro.application.privacy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.lmf.finpro.domain.model.DocumentType;
import com.lmf.finpro.domain.model.Household;
import com.lmf.finpro.domain.model.HouseholdMembership;
import com.lmf.finpro.domain.model.HouseholdRole;
import com.lmf.finpro.domain.model.HouseholdType;
import com.lmf.finpro.domain.model.LegalDocumentType;
import com.lmf.finpro.domain.model.PersonalDataExport;
import com.lmf.finpro.domain.model.TaxRegime;
import com.lmf.finpro.domain.model.User;
import com.lmf.finpro.domain.model.UserConsent;
import com.lmf.finpro.domain.port.out.HouseholdRepositoryPort;
import com.lmf.finpro.domain.port.out.PersonalDataArchiveWriterPort;
import com.lmf.finpro.domain.port.out.PersonalDataExportPort;
import com.lmf.finpro.domain.port.out.PersonalDataExportPort.StoredAttachment;
import com.lmf.finpro.domain.port.out.UserConsentRepositoryPort;
import com.lmf.finpro.domain.port.out.UserRepositoryPort;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DataExportApplicationServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long HOUSEHOLD_ID = 10L;

    @Mock private UserRepositoryPort userRepositoryPort;
    @Mock private HouseholdRepositoryPort householdRepositoryPort;
    @Mock private PersonalDataExportPort personalDataExportPort;
    @Mock private UserConsentRepositoryPort userConsentRepositoryPort;
    @Mock private PersonalDataArchiveWriterPort personalDataArchiveWriterPort;

    private DataExportApplicationService service;

    @BeforeEach
    void setUp() {
        Clock clock =
                Clock.fixed(Instant.parse("2026-10-07T15:00:00Z"), ZoneId.of("America/Sao_Paulo"));
        service =
                new DataExportApplicationService(
                        userRepositoryPort,
                        householdRepositoryPort,
                        personalDataExportPort,
                        userConsentRepositoryPort,
                        personalDataArchiveWriterPort,
                        clock);
    }

    private User user(String photoKey) {
        return new User(
                USER_ID,
                "Ana Freelancer",
                "ana@finpro.test",
                "hashed",
                DocumentType.CPF,
                "52998224725",
                "11987654321",
                TaxRegime.AUTONOMO,
                null,
                LocalDateTime.now(),
                0,
                photoKey,
                photoKey == null ? null : "image/png");
    }

    private void givenGroup(HouseholdType type) {
        when(householdRepositoryPort.findMembershipsByUserId(USER_ID))
                .thenReturn(
                        List.of(
                                new HouseholdMembership(
                                        HOUSEHOLD_ID, USER_ID, HouseholdRole.OWNER)));
        when(householdRepositoryPort.findById(HOUSEHOLD_ID))
                .thenReturn(
                        Optional.of(new Household(HOUSEHOLD_ID, "Ana", type, LocalDateTime.now())));
        when(personalDataExportPort.loadUser(USER_ID))
                .thenReturn(Map.of("email", "ana@finpro.test"));
        when(personalDataExportPort.loadNotificationPreferences(USER_ID))
                .thenReturn(Optional.empty());
        when(personalDataExportPort.loadPushDevices(USER_ID)).thenReturn(List.of());
        when(personalDataExportPort.loadMembers(HOUSEHOLD_ID)).thenReturn(List.of());
        when(userConsentRepositoryPort.findByUserId(USER_ID))
                .thenReturn(
                        List.of(
                                new UserConsent(
                                        LegalDocumentType.TERMS,
                                        "2026-10-07",
                                        LocalDateTime.of(2026, 10, 7, 9, 0))));
    }

    @Test
    @SuppressWarnings("unchecked")
    void bundlesUserGroupsConsentsAndTheirTables() {
        when(userRepositoryPort.findById(USER_ID)).thenReturn(Optional.of(user(null)));
        givenGroup(HouseholdType.PERSONAL);
        when(personalDataExportPort.loadHouseholdData(HOUSEHOLD_ID))
                .thenReturn(
                        Map.of("accounts", List.of(Map.of("id", 5L, "name", "Conta Principal"))));
        when(personalDataExportPort.loadAttachments(HOUSEHOLD_ID)).thenReturn(List.of());

        PersonalDataExport export = service.prepare(USER_ID);

        Map<String, Object> document = export.document();
        assertThat(document).containsKeys("versaoDoFormato", "geradoEm", "usuario", "grupos");
        assertThat(document.get("geradoEm")).isEqualTo(LocalDateTime.of(2026, 10, 7, 12, 0));
        assertThat((List<?>) document.get("consentimentos")).hasSize(1);
        List<Map<String, Object>> groups = (List<Map<String, Object>>) document.get("grupos");
        assertThat(groups).hasSize(1);
        assertThat(groups.get(0))
                .containsEntry("nome", "Ana")
                .containsEntry("seuPapel", HouseholdRole.OWNER);
        Map<String, Object> data = (Map<String, Object>) groups.get(0).get("dados");
        assertThat(data).containsKey("accounts");
        assertThat(export.files()).isEmpty();
        assertThat(export.readme()).doesNotContain("grupos compartilhados");
    }

    @Test
    @SuppressWarnings("unchecked")
    void attachmentsGetAPathInsideTheZipAndTheirRowsPointToIt() {
        when(userRepositoryPort.findById(USER_ID)).thenReturn(Optional.of(user(null)));
        givenGroup(HouseholdType.SHARED);
        when(personalDataExportPort.loadHouseholdData(HOUSEHOLD_ID))
                .thenReturn(
                        Map.of(
                                "transaction_attachments",
                                List.of(Map.of("id", 9L, "file_name", "../nota fiscal.pdf"))));
        when(personalDataExportPort.loadAttachments(HOUSEHOLD_ID))
                .thenReturn(List.of(new StoredAttachment(9L, "../nota fiscal.pdf", "uuid-1.pdf")));

        PersonalDataExport export = service.prepare(USER_ID);

        assertThat(export.files()).hasSize(1);
        PersonalDataExport.File file = export.files().get(0);
        assertThat(file.storageKey()).isEqualTo("uuid-1.pdf");
        assertThat(file.path()).startsWith("anexos/grupo-10/9-").doesNotContain("..");
        List<Map<String, Object>> groups =
                (List<Map<String, Object>>) export.document().get("grupos");
        Map<String, Object> data = (Map<String, Object>) groups.get(0).get("dados");
        List<Map<String, Object>> rows =
                (List<Map<String, Object>>) data.get("transaction_attachments");
        assertThat(rows.get(0)).containsEntry("arquivo_no_zip", file.path());
        assertThat(export.readme()).contains("grupos compartilhados");
    }

    @Test
    void profilePhotoIsIncludedWithItsExtension() {
        when(userRepositoryPort.findById(USER_ID))
                .thenReturn(Optional.of(user("profile-abc.webp")));
        givenGroup(HouseholdType.PERSONAL);
        when(personalDataExportPort.loadHouseholdData(HOUSEHOLD_ID)).thenReturn(Map.of());
        when(personalDataExportPort.loadAttachments(HOUSEHOLD_ID)).thenReturn(List.of());

        PersonalDataExport export = service.prepare(USER_ID);

        assertThat(export.files())
                .containsExactly(
                        new PersonalDataExport.File("foto-perfil.webp", "profile-abc.webp"));
    }

    @Test
    void safeFileNameStripsPathsAndLimitsLength() {
        assertThat(DataExportApplicationService.safeFileName("../../etc/passwd"))
                .doesNotContain("/")
                .doesNotContain("..");
        assertThat(DataExportApplicationService.safeFileName("a".repeat(300)).length())
                .isLessThanOrEqualTo(80);
        assertThat(DataExportApplicationService.safeFileName(null)).isEqualTo("arquivo");
        assertThat(DataExportApplicationService.safeFileName("Nota Fiscal (março).pdf"))
                .isEqualTo("Nota_Fiscal__mar_o_.pdf");
    }
}
