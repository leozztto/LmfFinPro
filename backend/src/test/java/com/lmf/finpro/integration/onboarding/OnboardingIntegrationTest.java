package com.lmf.finpro.integration.onboarding;

import static org.assertj.core.api.Assertions.assertThat;

import com.lmf.finpro.application.onboarding.ActivationEmailApplicationService;
import com.lmf.finpro.domain.model.AccountType;
import com.lmf.finpro.domain.model.OnboardingProgress;
import com.lmf.finpro.infrastructure.web.dto.account.AccountRequest;
import com.lmf.finpro.infrastructure.web.dto.account.AccountResponse;
import com.lmf.finpro.infrastructure.web.dto.importbatch.ImportBatchResponse;
import com.lmf.finpro.infrastructure.web.dto.importbatch.ImportSummaryResponse;
import com.lmf.finpro.infrastructure.web.dto.onboarding.ActivationEmailsRequest;
import com.lmf.finpro.infrastructure.web.dto.onboarding.OnboardingResponse;
import com.lmf.finpro.integration.support.AbstractIntegrationTest;
import com.lmf.finpro.integration.support.TestDataFactory;
import com.lmf.finpro.integration.support.TestUser;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
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

class OnboardingIntegrationTest extends AbstractIntegrationTest {

    private static final String CSV =
            """
            date,description,amount
            2026-01-05,UBER *TRIP,-32.50
            2026-01-06,PAGAMENTO CLIENTE ACME,4200.00
            2026-01-07,PADARIA,-17.50
            """;

    @Autowired private JdbcTemplate jdbc;
    @Autowired private ActivationEmailApplicationService activationEmails;

    @Test
    void guideStartsEmptyAndEachStepSeenIsRecorded() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);

        OnboardingResponse start = progress(user);
        assertThat(start.totalCount()).isEqualTo(14);
        assertThat(start.completedCount()).isZero();
        assertThat(start.completed()).isFalse();
        assertThat(start.dismissed()).isFalse();
        assertThat(start.activationEmailsEnabled()).isTrue();

        completeStep(user, "ACCOUNT_OPEN");
        completeStep(user, "ACCOUNT_OPEN");
        completeStep(user, "ACCOUNT_FILL");

        OnboardingResponse resumed = progress(user);
        assertThat(resumed.completedCount()).isEqualTo(2);
        assertThat(stepDone(resumed, "ACCOUNT_OPEN")).isTrue();
        assertThat(stepDone(resumed, "ACCOUNT_FILL")).isTrue();
        assertThat(stepDone(resumed, "ACCOUNT_SAVE")).isFalse();
        assertThat(resumed.completed()).isFalse();
    }

    @Test
    void finishingEveryStepCompletesTheGuideOnlyForThatUser() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        TestUser other = TestDataFactory.registerRandomUser(restTemplate);

        for (OnboardingProgress.StepId step : OnboardingProgress.StepId.values()) {
            completeStep(user, step.name());
        }

        assertThat(progress(user).completed()).isTrue();
        assertThat(progress(other).completedCount()).isZero();
        Integer recorded =
                jdbc.queryForObject(
                        "SELECT count(*) FROM onboarding_steps_done WHERE user_id = ?"
                                + " AND completed_at IS NOT NULL",
                        Integer.class,
                        user.userId());
        assertThat(recorded).isEqualTo(14);
    }

    @Test
    void unknownStepIsRejected() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);

        ResponseEntity<String> response =
                restTemplate.exchange(
                        "/api/onboarding/steps/NAO_EXISTE",
                        HttpMethod.POST,
                        new HttpEntity<>(user.authHeaders()),
                        String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void dismissAndEmailChoiceArePersisted() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);

        ResponseEntity<Void> dismiss =
                restTemplate.exchange(
                        "/api/onboarding/dismiss",
                        HttpMethod.POST,
                        new HttpEntity<>(user.authHeaders()),
                        Void.class);
        ResponseEntity<Void> optOut =
                restTemplate.exchange(
                        "/api/onboarding/activation-emails",
                        HttpMethod.PUT,
                        new HttpEntity<>(new ActivationEmailsRequest(false), user.authHeaders()),
                        Void.class);

        assertThat(dismiss.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(optOut.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        OnboardingResponse state = progress(user);
        assertThat(state.dismissed()).isTrue();
        assertThat(state.activationEmailsEnabled()).isFalse();
    }

    @Test
    void onboardingRequiresAuthentication() {
        ResponseEntity<String> response =
                restTemplate.getForEntity("/api/onboarding", String.class);

        assertThat(response.getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
    }

    @Test
    void writingEndpointsRequireAuthentication() {
        for (String path :
                new String[] {"/api/onboarding/steps/ACCOUNT_OPEN", "/api/onboarding/dismiss"}) {
            ResponseEntity<String> response = restTemplate.postForEntity(path, null, String.class);

            assertThat(response.getStatusCode())
                    .as(path)
                    .isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
        }
        ResponseEntity<String> optOut =
                restTemplate.exchange(
                        "/api/onboarding/activation-emails",
                        HttpMethod.PUT,
                        new HttpEntity<>(new ActivationEmailsRequest(false)),
                        String.class);
        assertThat(optOut.getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
    }

    @Test
    void dismissedGuideKeepsTheStepsAlreadySeen() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        completeStep(user, "ACCOUNT_OPEN");

        restTemplate.exchange(
                "/api/onboarding/dismiss",
                HttpMethod.POST,
                new HttpEntity<>(user.authHeaders()),
                Void.class);

        OnboardingResponse state = progress(user);
        assertThat(state.dismissed()).isTrue();
        assertThat(state.completedCount()).isEqualTo(1);
        assertThat(state.completed()).isFalse();
    }

    @Test
    void importSummaryShowsTheFirstResult() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        Long accountId = createAccount(user);
        ImportBatchResponse batch = upload(user, accountId);

        ResponseEntity<ImportSummaryResponse> response =
                restTemplate.exchange(
                        "/api/import-batches/" + batch.id() + "/summary",
                        HttpMethod.GET,
                        new HttpEntity<>(user.authHeaders()),
                        ImportSummaryResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ImportSummaryResponse summary = response.getBody();
        assertThat(summary.transactionCount()).isEqualTo(3);
        assertThat(summary.totalIncome()).isEqualByComparingTo("4200.00");
        assertThat(summary.totalExpense()).isEqualByComparingTo("50.00");
        assertThat(summary.balance()).isEqualByComparingTo("4150.00");
        assertThat(summary.uncategorizedCount()).isEqualTo(batch.uncategorizedCount());
        assertThat(summary.topExpenseCategories()).isNotEmpty();
        assertThat(summary.topExpenseCategories())
                .extracting(ImportSummaryResponse.CategoryTotalResponse::total)
                .allSatisfy(total -> assertThat(total.signum()).isPositive());
    }

    @Test
    void importSummaryOfAnotherUsersBatchIsNotFound() {
        TestUser owner = TestDataFactory.registerRandomUser(restTemplate);
        TestUser stranger = TestDataFactory.registerRandomUser(restTemplate);
        ImportBatchResponse batch = upload(owner, createAccount(owner));

        ResponseEntity<String> response =
                restTemplate.exchange(
                        "/api/import-batches/" + batch.id() + "/summary",
                        HttpMethod.GET,
                        new HttpEntity<>(stranger.authHeaders()),
                        String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void welcomeEmailIsRecordedOnceAndSkippedForThoseWhoOptedOut() {
        TestUser welcomed = TestDataFactory.registerRandomUser(restTemplate);
        TestUser optedOut = TestDataFactory.registerRandomUser(restTemplate);
        restTemplate.exchange(
                "/api/onboarding/activation-emails",
                HttpMethod.PUT,
                new HttpEntity<>(new ActivationEmailsRequest(false), optedOut.authHeaders()),
                Void.class);

        activationEmails.sendDue();
        activationEmails.sendDue();

        assertThat(sentKinds(welcomed.userId())).containsExactly("WELCOME");
        assertThat(sentKinds(optedOut.userId())).isEmpty();
    }

    private java.util.List<String> sentKinds(Long userId) {
        return jdbc.queryForList(
                "SELECT kind FROM activation_emails_sent WHERE user_id = ?", String.class, userId);
    }

    private static boolean stepDone(OnboardingResponse response, String id) {
        return response.steps().stream()
                .filter(step -> step.id().equals(id))
                .findFirst()
                .orElseThrow()
                .done();
    }

    private void completeStep(TestUser user, String step) {
        ResponseEntity<Void> response =
                restTemplate.exchange(
                        "/api/onboarding/steps/" + step,
                        HttpMethod.POST,
                        new HttpEntity<>(user.authHeaders()),
                        Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    private OnboardingResponse progress(TestUser user) {
        return restTemplate
                .exchange(
                        "/api/onboarding",
                        HttpMethod.GET,
                        new HttpEntity<>(user.authHeaders()),
                        OnboardingResponse.class)
                .getBody();
    }

    private Long createAccount(TestUser user) {
        ResponseEntity<AccountResponse> response =
                restTemplate.exchange(
                        "/api/accounts",
                        HttpMethod.POST,
                        new HttpEntity<>(
                                new AccountRequest(
                                        "Conta corrente", AccountType.CHECKING, BigDecimal.ZERO),
                                user.authHeaders()),
                        AccountResponse.class);
        return response.getBody().id();
    }

    private ImportBatchResponse upload(TestUser user, Long accountId) {
        HttpHeaders headers = user.authHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("accountId", accountId.toString());
        body.add(
                "file",
                new ByteArrayResource(CSV.getBytes(StandardCharsets.UTF_8)) {
                    @Override
                    public String getFilename() {
                        return "extrato.csv";
                    }
                });
        return restTemplate
                .exchange(
                        "/api/import-batches",
                        HttpMethod.POST,
                        new HttpEntity<>(body, headers),
                        ImportBatchResponse.class)
                .getBody();
    }
}
