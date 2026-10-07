package com.lmf.finpro.integration.privacy;

import static org.assertj.core.api.Assertions.assertThat;

import com.lmf.finpro.application.legal.LegalDocuments;
import com.lmf.finpro.domain.model.AccountType;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.DocumentType;
import com.lmf.finpro.domain.model.HouseholdType;
import com.lmf.finpro.domain.model.SavingsGoalType;
import com.lmf.finpro.domain.model.TaxRegime;
import com.lmf.finpro.infrastructure.security.JwtAuthenticationFilter;
import com.lmf.finpro.infrastructure.web.dto.account.AccountRequest;
import com.lmf.finpro.infrastructure.web.dto.account.AccountResponse;
import com.lmf.finpro.infrastructure.web.dto.attachment.TransactionAttachmentResponse;
import com.lmf.finpro.infrastructure.web.dto.auth.AuthResponse;
import com.lmf.finpro.infrastructure.web.dto.auth.LoginRequest;
import com.lmf.finpro.infrastructure.web.dto.auth.RegisterRequest;
import com.lmf.finpro.infrastructure.web.dto.household.HouseholdRequest;
import com.lmf.finpro.infrastructure.web.dto.household.HouseholdResponse;
import com.lmf.finpro.infrastructure.web.dto.legal.AcceptConsentRequest;
import com.lmf.finpro.infrastructure.web.dto.legal.ConsentStatusResponse;
import com.lmf.finpro.infrastructure.web.dto.legal.LegalVersionsResponse;
import com.lmf.finpro.infrastructure.web.dto.privacy.AccountDeletionPreviewResponse;
import com.lmf.finpro.infrastructure.web.dto.privacy.DeleteAccountRequest;
import com.lmf.finpro.infrastructure.web.dto.savingsgoal.SavingsGoalRequest;
import com.lmf.finpro.infrastructure.web.dto.savingsgoal.SavingsGoalResponse;
import com.lmf.finpro.infrastructure.web.dto.transaction.TransactionRequest;
import com.lmf.finpro.infrastructure.web.dto.transaction.TransactionResponse;
import com.lmf.finpro.infrastructure.web.dto.transfer.TransferRequest;
import com.lmf.finpro.infrastructure.web.exception.ApiError;
import com.lmf.finpro.integration.support.AbstractIntegrationTest;
import com.lmf.finpro.integration.support.CpfTestFactory;
import com.lmf.finpro.integration.support.TestDataFactory;
import com.lmf.finpro.integration.support.TestUser;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/** LGPD de ponta a ponta: aceite dos documentos, exportação dos dados e exclusão da conta. */
class PrivacyIntegrationTest extends AbstractIntegrationTest {

    private static final String PASSWORD = "senha12345";
    private static final LocalDate DATE = LocalDate.of(2026, 3, 10);

    @Autowired private JdbcTemplate jdbc;

    /** O que {@link #seedRichData} criou, para conferir depois. */
    private record Seed(Long householdId, byte[] attachment, byte[] photo, Long goalId) {}

    // ---------- aceite dos documentos ----------

    @Test
    void legalVersionsArePublic() {
        ResponseEntity<LegalVersionsResponse> response =
                restTemplate.getForEntity("/api/legal/versions", LegalVersionsResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().termsVersion()).isEqualTo(LegalDocuments.TERMS_VERSION);
        assertThat(response.getBody().privacyVersion()).isEqualTo(LegalDocuments.PRIVACY_VERSION);
    }

    @Test
    void registrationWithoutAcceptedVersionsIsRejected() {
        RegisterRequest request = registerWithVersions(null, null);

        ResponseEntity<ApiError> response =
                restTemplate.postForEntity("/api/auth/register", request, ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void registrationWithAnOutdatedVersionIsRejectedAndCreatesNothing() {
        RegisterRequest request =
                registerWithVersions("1999-01-01", LegalDocuments.PRIVACY_VERSION);

        ResponseEntity<ApiError> response =
                restTemplate.postForEntity("/api/auth/register", request, ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM users WHERE email = ?",
                                Integer.class,
                                request.email()))
                .isZero();
    }

    @Test
    void registrationRecordsTheAcceptanceOfBothDocuments() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);

        ConsentStatusResponse status = consentStatus(user);

        assertThat(status.pending()).isFalse();
        assertThat(status.terms().accepted()).isTrue();
        assertThat(status.terms().acceptedVersion()).isEqualTo(LegalDocuments.TERMS_VERSION);
        assertThat(status.terms().acceptedAt()).isNotNull();
        assertThat(status.privacy().accepted()).isTrue();
    }

    @Test
    void userWithoutAcceptanceIsPendingUntilAcceptingTheCurrentVersions() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        // Simula quem já tinha conta antes de os documentos existirem.
        jdbc.update("DELETE FROM user_consents WHERE user_id = ?", user.userId());
        assertThat(consentStatus(user).pending()).isTrue();

        ResponseEntity<ApiError> outdated =
                restTemplate.exchange(
                        "/api/consents",
                        HttpMethod.POST,
                        new HttpEntity<>(
                                new AcceptConsentRequest("1999-01-01", "1999-01-01"),
                                user.authHeaders()),
                        ApiError.class);
        assertThat(outdated.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(consentStatus(user).pending()).isTrue();

        ResponseEntity<ConsentStatusResponse> accepted =
                restTemplate.exchange(
                        "/api/consents",
                        HttpMethod.POST,
                        new HttpEntity<>(
                                new AcceptConsentRequest(
                                        LegalDocuments.TERMS_VERSION,
                                        LegalDocuments.PRIVACY_VERSION),
                                user.authHeaders()),
                        ConsentStatusResponse.class);

        assertThat(accepted.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(accepted.getBody().pending()).isFalse();
        assertThat(consentStatus(user).pending()).isFalse();
    }

    @Test
    void acceptingTwiceKeepsASingleRecordPerVersion() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        AcceptConsentRequest request =
                new AcceptConsentRequest(
                        LegalDocuments.TERMS_VERSION, LegalDocuments.PRIVACY_VERSION);

        restTemplate.exchange(
                "/api/consents",
                HttpMethod.POST,
                new HttpEntity<>(request, user.authHeaders()),
                ConsentStatusResponse.class);

        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM user_consents WHERE user_id = ?",
                                Integer.class,
                                user.userId()))
                .isEqualTo(2);
    }

    @Test
    void consentEndpointsRequireAuthentication() {
        assertThat(restTemplate.getForEntity("/api/consents", String.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ---------- exportação ----------

    @Test
    void exportBundlesTheDataTheAttachmentAndThePhotoWithoutSecrets() throws IOException {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        Seed seed = seedRichData(user);

        ResponseEntity<byte[]> response =
                restTemplate.exchange(
                        "/api/privacy/export",
                        HttpMethod.GET,
                        new HttpEntity<>(user.authHeaders()),
                        byte[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType().toString()).isEqualTo("application/zip");
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .contains("finpro-meus-dados-");

        Map<String, byte[]> entries = unzip(response.getBody());
        assertThat(entries).containsKeys("dados.json", "LEIA-ME.txt");
        String json = new String(entries.get("dados.json"), StandardCharsets.UTF_8);
        assertThat(json)
                .contains(user.email())
                .contains("Conta Principal")
                .contains("Aluguel escritório")
                .contains("Caixinha do imposto")
                .contains("consentimentos")
                .contains(LegalDocuments.TERMS_VERSION)
                .doesNotContain("password_hash")
                .doesNotContain("session_version")
                .doesNotContain("storage_key")
                .doesNotContain("photo_key");

        String attachmentPath =
                entries.keySet().stream()
                        .filter(name -> name.startsWith("anexos/grupo-" + seed.householdId() + "/"))
                        .findFirst()
                        .orElseThrow();
        assertThat(entries.get(attachmentPath)).isEqualTo(seed.attachment());
        assertThat(json).contains(attachmentPath);
        assertThat(entries.keySet().stream().anyMatch(name -> name.startsWith("foto-perfil.")))
                .isTrue();
    }

    @Test
    void exportRequiresAuthentication() {
        assertThat(restTemplate.getForEntity("/api/privacy/export", String.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void exportOnlyContainsTheRequestersOwnData() throws IOException {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        TestUser stranger = TestDataFactory.registerRandomUser(restTemplate);
        createAccount(stranger, null, "Conta Secreta do Outro");

        ResponseEntity<byte[]> response =
                restTemplate.exchange(
                        "/api/privacy/export",
                        HttpMethod.GET,
                        new HttpEntity<>(user.authHeaders()),
                        byte[].class);

        String json =
                new String(unzip(response.getBody()).get("dados.json"), StandardCharsets.UTF_8);
        assertThat(json).doesNotContain("Conta Secreta do Outro").doesNotContain(stranger.email());
    }

    // ---------- exclusão da conta ----------

    @Test
    void deletionPreviewListsThePersonalSpaceAndItsAttachments() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        seedRichData(user);

        ResponseEntity<AccountDeletionPreviewResponse> response =
                restTemplate.exchange(
                        "/api/privacy/account-deletion-preview",
                        HttpMethod.GET,
                        new HttpEntity<>(user.authHeaders()),
                        AccountDeletionPreviewResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        AccountDeletionPreviewResponse preview = response.getBody();
        assertThat(preview.canDelete()).isTrue();
        assertThat(preview.blockers()).isEmpty();
        assertThat(preview.deletedGroups()).hasSize(1);
        assertThat(preview.deletedGroups().get(0).type()).isEqualTo(HouseholdType.PERSONAL);
        assertThat(preview.leftGroups()).isEmpty();
        assertThat(preview.attachmentCount()).isEqualTo(1);
    }

    @Test
    void deletionWithTheWrongPasswordChangesNothing() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        Seed seed = seedRichData(user);

        ResponseEntity<ApiError> response = deleteAccount(user, "senha-errada");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(countRows("users", "id", user.userId())).isEqualTo(1);
        assertThat(countRows("accounts", "household_id", seed.householdId())).isEqualTo(2);
    }

    @Test
    void deletionWithoutAPasswordIsRejected() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);

        ResponseEntity<ApiError> response = deleteAccount(user, "");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(countRows("users", "id", user.userId())).isEqualTo(1);
    }

    @Test
    void deletionRemovesEveryTraceOfTheAccount() throws IOException {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        Seed seed = seedRichData(user);
        assertThat(storedFileWith(seed.attachment())).isTrue();
        assertThat(storedFileWith(seed.photo())).isTrue();

        ResponseEntity<ApiError> response = deleteAccount(user, PASSWORD);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE)).contains("Max-Age=0");

        // O banco: usuário, espaço pessoal e todas as tabelas de dados do grupo.
        assertThat(countRows("users", "id", user.userId())).isZero();
        assertThat(countRows("households", "id", seed.householdId())).isZero();
        assertThat(countRows("household_members", "user_id", user.userId())).isZero();
        assertThat(countRows("user_consents", "user_id", user.userId())).isZero();
        assertThat(countRows("refresh_tokens", "user_id", user.userId())).isZero();
        for (String table :
                List.of(
                        "accounts",
                        "categories",
                        "clients",
                        "tags",
                        "transfers",
                        "savings_goals",
                        "transaction_attachments",
                        "budgets",
                        "recurring_transactions")) {
            assertThat(countRows(table, "household_id", seed.householdId()))
                    .as("linhas de %s do grupo apagado", table)
                    .isZero();
        }
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM transactions t WHERE NOT EXISTS (SELECT 1"
                                        + " FROM accounts a WHERE a.id = t.account_id)",
                                Integer.class))
                .isZero();
        assertThat(countRows("account_deletion_log", "user_id", user.userId())).isEqualTo(1);

        // O armazenamento: anexo e foto saem do disco.
        assertThat(storedFileWith(seed.attachment())).isFalse();
        assertThat(storedFileWith(seed.photo())).isFalse();

        // A sessão: o token antigo e o login deixam de funcionar.
        assertThat(
                        restTemplate
                                .exchange(
                                        "/api/profile",
                                        HttpMethod.GET,
                                        new HttpEntity<>(user.authHeaders()),
                                        String.class)
                                .getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(
                        restTemplate
                                .postForEntity(
                                        "/api/auth/login",
                                        new LoginRequest(user.email(), PASSWORD),
                                        String.class)
                                .getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void deletionDoesNotTouchAnotherUsersData() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        TestUser other = TestDataFactory.registerRandomUser(restTemplate);
        seedRichData(user);
        Long otherAccountId = createAccount(other, null, "Conta do Outro");

        deleteAccount(user, PASSWORD);

        assertThat(countRows("accounts", "id", otherAccountId)).isEqualTo(1);
        assertThat(countRows("users", "id", other.userId())).isEqualTo(1);
    }

    @Test
    void emailCanBeReusedAfterTheAccountIsDeleted() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        deleteAccount(user, PASSWORD);

        ResponseEntity<AuthResponse> again =
                restTemplate.postForEntity(
                        "/api/auth/register",
                        registerRequest(user.email(), CpfTestFactory.randomValidCpf()),
                        AuthResponse.class);

        assertThat(again.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    // ---------- exclusão e grupos compartilhados ----------

    @Test
    void ownerOfASharedGroupWithOtherMembersCannotDeleteTheAccount() {
        TestUser owner = TestDataFactory.registerRandomUser(restTemplate);
        TestUser member = TestDataFactory.registerRandomUser(restTemplate);
        Long groupId = createSharedGroup(owner, "Casa");
        addMember(groupId, member);

        ResponseEntity<AccountDeletionPreviewResponse> preview =
                restTemplate.exchange(
                        "/api/privacy/account-deletion-preview",
                        HttpMethod.GET,
                        new HttpEntity<>(owner.authHeaders()),
                        AccountDeletionPreviewResponse.class);
        assertThat(preview.getBody().canDelete()).isFalse();
        assertThat(preview.getBody().blockers()).singleElement().asString().contains("Casa");

        ResponseEntity<ApiError> response = deleteAccount(owner, PASSWORD);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(countRows("users", "id", owner.userId())).isEqualTo(1);
        assertThat(countRows("households", "id", groupId)).isEqualTo(1);
    }

    @Test
    void memberLeavesTheGroupAndTheGroupKeepsItsDataWithoutAnAuthor() {
        TestUser owner = TestDataFactory.registerRandomUser(restTemplate);
        TestUser member = TestDataFactory.registerRandomUser(restTemplate);
        Long groupId = createSharedGroup(owner, "Casa");
        addMember(groupId, member);
        Long memberAccountId = createAccount(member, groupId, "Conta trazida pela pessoa");
        Long memberTransactionId = createExpense(member, groupId, memberAccountId, "Mercado");
        assertThat(
                        jdbc.queryForObject(
                                "SELECT created_by FROM transactions WHERE id = ?",
                                Long.class,
                                memberTransactionId))
                .isEqualTo(member.userId());

        ResponseEntity<AccountDeletionPreviewResponse> preview =
                restTemplate.exchange(
                        "/api/privacy/account-deletion-preview",
                        HttpMethod.GET,
                        new HttpEntity<>(member.authHeaders()),
                        AccountDeletionPreviewResponse.class);
        assertThat(preview.getBody().canDelete()).isTrue();
        assertThat(preview.getBody().leftGroups()).hasSize(1);
        assertThat(preview.getBody().leftGroups().get(0).accountsBroughtByYou()).isEqualTo(1);

        ResponseEntity<ApiError> response = deleteAccount(member, PASSWORD);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(countRows("users", "id", member.userId())).isZero();
        assertThat(countRows("household_members", "user_id", member.userId())).isZero();
        // O grupo e os dados continuam, sem o nome da pessoa.
        assertThat(countRows("households", "id", groupId)).isEqualTo(1);
        assertThat(countRows("accounts", "id", memberAccountId)).isEqualTo(1);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT owner_user_id FROM accounts WHERE id = ?",
                                Long.class,
                                memberAccountId))
                .isNull();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT created_by FROM transactions WHERE id = ?",
                                Long.class,
                                memberTransactionId))
                .isNull();
        // O dono continua entrando no grupo normalmente.
        assertThat(
                        restTemplate
                                .exchange(
                                        "/api/accounts",
                                        HttpMethod.GET,
                                        new HttpEntity<>(headers(owner, groupId)),
                                        String.class)
                                .getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    @Test
    void sharedGroupWithASingleMemberIsDeletedWithTheAccount() {
        TestUser owner = TestDataFactory.registerRandomUser(restTemplate);
        Long groupId = createSharedGroup(owner, "Só eu");
        createAccount(owner, groupId, "Conta do grupo");

        ResponseEntity<ApiError> response = deleteAccount(owner, PASSWORD);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(countRows("households", "id", groupId)).isZero();
        assertThat(countRows("accounts", "household_id", groupId)).isZero();
    }

    @Test
    void pendingInvitesAddressedToTheDeletedEmailAreRemoved() {
        TestUser owner = TestDataFactory.registerRandomUser(restTemplate);
        TestUser invited = TestDataFactory.registerRandomUser(restTemplate);
        Long groupId = createSharedGroup(owner, "Casa");
        jdbc.update(
                "INSERT INTO household_invites (household_id, email, token_hash, created_by,"
                        + " expires_at) VALUES (?, ?, ?, ?, now() + interval '7 days')",
                groupId,
                invited.email().toUpperCase(),
                UUID.randomUUID().toString().replace("-", "")
                        + UUID.randomUUID().toString().replace("-", ""),
                owner.userId());

        deleteAccount(invited, PASSWORD);

        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM household_invites WHERE household_id = ?",
                                Integer.class,
                                groupId))
                .isZero();
    }

    // ---------- apoio ----------

    private ConsentStatusResponse consentStatus(TestUser user) {
        return restTemplate
                .exchange(
                        "/api/consents",
                        HttpMethod.GET,
                        new HttpEntity<>(user.authHeaders()),
                        ConsentStatusResponse.class)
                .getBody();
    }

    private ResponseEntity<ApiError> deleteAccount(TestUser user, String password) {
        return restTemplate.exchange(
                "/api/privacy/account",
                HttpMethod.DELETE,
                new HttpEntity<>(new DeleteAccountRequest(password), user.authHeaders()),
                ApiError.class);
    }

    private int countRows(String table, String column, Object value) {
        Integer count =
                jdbc.queryForObject(
                        "SELECT count(*) FROM " + table + " WHERE " + column + " = ?",
                        Integer.class,
                        value);
        return count == null ? 0 : count;
    }

    private HttpHeaders headers(TestUser user, Long householdId) {
        HttpHeaders headers = user.authHeaders();
        if (householdId != null) {
            headers.add(JwtAuthenticationFilter.HOUSEHOLD_HEADER, String.valueOf(householdId));
        }
        return headers;
    }

    private RegisterRequest registerRequest(String email, String cpf) {
        return new RegisterRequest(
                "Usuário Teste",
                email,
                PASSWORD,
                DocumentType.CPF,
                cpf,
                "11987654321",
                TaxRegime.AUTONOMO,
                TestDataFactory.sampleAddress());
    }

    private RegisterRequest registerWithVersions(String termsVersion, String privacyVersion) {
        RegisterRequest base =
                registerRequest(
                        "user-" + UUID.randomUUID() + "@finpro.test",
                        CpfTestFactory.randomValidCpf());
        return new RegisterRequest(
                base.name(),
                base.email(),
                base.password(),
                base.documentType(),
                base.documentNumber(),
                base.phone(),
                base.taxRegime(),
                base.address(),
                null,
                termsVersion,
                privacyVersion);
    }

    /** Duas contas, uma despesa com comprovante, uma transferência, uma meta e a foto de perfil. */
    private Seed seedRichData(TestUser user) {
        Long householdId = personalHouseholdId(user);
        Long main = createAccount(user, null, "Conta Principal");
        Long reserve = createAccount(user, null, "Conta Reserva", AccountType.RESERVE);
        Long transactionId = createExpense(user, null, main, "Aluguel escritório");

        byte[] attachment = uniquePng();
        TransactionAttachmentResponse uploaded = upload(user, transactionId, attachment);
        assertThat(uploaded).isNotNull();

        restTemplate.exchange(
                "/api/transfers",
                HttpMethod.POST,
                new HttpEntity<>(
                        new TransferRequest(main, reserve, BigDecimal.TEN, DATE, "Reserva"),
                        user.authHeaders()),
                String.class);
        Long goalId =
                restTemplate
                        .exchange(
                                "/api/savings-goals",
                                HttpMethod.POST,
                                new HttpEntity<>(
                                        new SavingsGoalRequest(
                                                "Caixinha do imposto",
                                                SavingsGoalType.TAX_RESERVE,
                                                BigDecimal.valueOf(10000),
                                                null,
                                                new BigDecimal("0.06"),
                                                false,
                                                reserve,
                                                main),
                                        user.authHeaders()),
                                SavingsGoalResponse.class)
                        .getBody()
                        .id();

        byte[] photo = uniquePng();
        putPhoto(user, photo);
        return new Seed(householdId, attachment, photo, goalId);
    }

    private Long personalHouseholdId(TestUser user) {
        HouseholdResponse[] households =
                restTemplate
                        .exchange(
                                "/api/households",
                                HttpMethod.GET,
                                new HttpEntity<>(user.authHeaders()),
                                HouseholdResponse[].class)
                        .getBody();
        return Arrays.stream(households)
                .filter(household -> household.type() == HouseholdType.PERSONAL)
                .findFirst()
                .orElseThrow()
                .id();
    }

    private Long createSharedGroup(TestUser owner, String name) {
        return restTemplate
                .exchange(
                        "/api/households",
                        HttpMethod.POST,
                        new HttpEntity<>(new HouseholdRequest(name), owner.authHeaders()),
                        HouseholdResponse.class)
                .getBody()
                .id();
    }

    /** Entrada direta no grupo: o convite por e-mail já é coberto pelos testes de grupos. */
    private void addMember(Long groupId, TestUser member) {
        jdbc.update(
                "INSERT INTO household_members (household_id, user_id, role) VALUES (?, ?,"
                        + " 'MEMBER')",
                groupId,
                member.userId());
    }

    private Long createAccount(TestUser user, Long householdId, String name) {
        return createAccount(user, householdId, name, AccountType.CHECKING);
    }

    private Long createAccount(TestUser user, Long householdId, String name, AccountType type) {
        return restTemplate
                .exchange(
                        "/api/accounts",
                        HttpMethod.POST,
                        new HttpEntity<>(
                                new AccountRequest(name, type, BigDecimal.valueOf(1000)),
                                headers(user, householdId)),
                        AccountResponse.class)
                .getBody()
                .id();
    }

    private Long createExpense(
            TestUser user, Long householdId, Long accountId, String description) {
        return restTemplate
                .exchange(
                        "/api/transactions",
                        HttpMethod.POST,
                        new HttpEntity<>(
                                new TransactionRequest(
                                        accountId,
                                        null,
                                        null,
                                        description,
                                        BigDecimal.TEN,
                                        DATE,
                                        CategoryType.EXPENSE),
                                headers(user, householdId)),
                        TransactionResponse.class)
                .getBody()
                .id();
    }

    private TransactionAttachmentResponse upload(TestUser user, Long transactionId, byte[] png) {
        HttpHeaders headers = user.authHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", namedResource(png, "comprovante.png"));
        body.add("documentType", "PAYMENT_PROOF");
        return restTemplate
                .exchange(
                        "/api/transactions/" + transactionId + "/attachments",
                        HttpMethod.POST,
                        new HttpEntity<>(body, headers),
                        TransactionAttachmentResponse.class)
                .getBody();
    }

    private void putPhoto(TestUser user, byte[] png) {
        HttpHeaders headers = user.authHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", namedResource(png, "foto.png"));
        ResponseEntity<String> response =
                restTemplate.exchange(
                        "/api/profile/photo",
                        HttpMethod.PUT,
                        new HttpEntity<>(body, headers),
                        String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private static ByteArrayResource namedResource(byte[] content, String fileName) {
        return new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return fileName;
            }
        };
    }

    private static byte[] uniquePng() {
        byte[] signature = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        byte[] marker = UUID.randomUUID().toString().getBytes(StandardCharsets.US_ASCII);
        byte[] content = Arrays.copyOf(signature, signature.length + marker.length);
        System.arraycopy(marker, 0, content, signature.length, marker.length);
        return content;
    }

    private static boolean storedFileWith(byte[] content) throws IOException {
        try (Stream<Path> files = Files.list(ATTACHMENTS_DIR)) {
            return files.anyMatch(
                    file -> {
                        try {
                            return Arrays.equals(Files.readAllBytes(file), content);
                        } catch (IOException e) {
                            return false;
                        }
                    });
        }
    }

    private static Map<String, byte[]> unzip(byte[] zip) throws IOException {
        Map<String, byte[]> entries = new HashMap<>();
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                entries.put(entry.getName(), in.readAllBytes());
            }
        }
        return entries;
    }
}
