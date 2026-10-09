package com.lmf.finpro.integration.onboarding;

import static org.assertj.core.api.Assertions.assertThat;

import com.lmf.finpro.domain.model.AccountType;
import com.lmf.finpro.domain.model.ActivationCandidate;
import com.lmf.finpro.domain.model.ActivationEmailKind;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.port.out.ActivationCandidatePort;
import com.lmf.finpro.infrastructure.web.dto.account.AccountRequest;
import com.lmf.finpro.infrastructure.web.dto.account.AccountResponse;
import com.lmf.finpro.infrastructure.web.dto.onboarding.ActivationEmailsRequest;
import com.lmf.finpro.infrastructure.web.dto.transaction.TransactionRequest;
import com.lmf.finpro.integration.support.AbstractIntegrationTest;
import com.lmf.finpro.integration.support.TestDataFactory;
import com.lmf.finpro.integration.support.TestUser;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** A consulta que decide quem pode receber e-mail de ativação, contra o banco de verdade. */
class ActivationCandidatesIntegrationTest extends AbstractIntegrationTest {

    @Autowired private ActivationCandidatePort candidatePort;

    private Optional<ActivationCandidate> candidate(TestUser user) {
        return candidatePort.findCandidates(LocalDateTime.now().minusDays(1)).stream()
                .filter(c -> c.userId().equals(user.userId()))
                .findFirst();
    }

    @Test
    void newUserIsACandidateWithoutTransactionsAndWithTheGuideOpen() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);

        ActivationCandidate found = candidate(user).orElseThrow();

        assertThat(found.email()).isEqualTo(user.email());
        assertThat(found.hasTransactions()).isFalse();
        assertThat(found.guideDismissed()).isFalse();
        assertThat(found.sentKinds()).isEmpty();
    }

    @Test
    void dismissingTheGuideIsReflectedButTheUserStaysACandidate() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);

        post("/api/onboarding/dismiss", user);

        ActivationCandidate found = candidate(user).orElseThrow();
        assertThat(found.guideDismissed()).isTrue();
    }

    @Test
    void whoOptedOutOfTheEmailsIsNotACandidate() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);

        ResponseEntity<Void> response =
                restTemplate.exchange(
                        "/api/onboarding/activation-emails",
                        HttpMethod.PUT,
                        new HttpEntity<>(new ActivationEmailsRequest(false), user.authHeaders()),
                        Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(candidate(user)).isEmpty();
    }

    @Test
    void whoOptedBackInIsACandidateAgain() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        setEmails(user, false);
        setEmails(user, true);

        assertThat(candidate(user)).isPresent();
    }

    @Test
    void signupsOlderThanTheLookbackAreIgnored() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);

        boolean found =
                candidatePort.findCandidates(LocalDateTime.now().plusMinutes(1)).stream()
                        .anyMatch(c -> c.userId().equals(user.userId()));

        assertThat(found).isFalse();
    }

    @Test
    void aFirstTransactionMarksTheUserAsActivated() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        Long accountId = createAccount(user);

        ResponseEntity<String> created =
                restTemplate.exchange(
                        "/api/transactions",
                        HttpMethod.POST,
                        new HttpEntity<>(
                                new TransactionRequest(
                                        accountId,
                                        null,
                                        null,
                                        "Padaria",
                                        new BigDecimal("25.00"),
                                        LocalDate.now(),
                                        CategoryType.EXPENSE),
                                user.authHeaders()),
                        String.class);
        assertThat(created.getStatusCode().is2xxSuccessful()).isTrue();

        assertThat(candidate(user).orElseThrow().hasTransactions()).isTrue();
    }

    @Test
    void markingAsSentIsRememberedAndRepeatingItIsHarmless() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);

        candidatePort.markSent(user.userId(), ActivationEmailKind.WELCOME);
        candidatePort.markSent(user.userId(), ActivationEmailKind.WELCOME);
        candidatePort.markSent(user.userId(), ActivationEmailKind.WEEK_ONE_CHECK_IN);

        assertThat(candidate(user).orElseThrow().sentKinds())
                .containsExactlyInAnyOrder(
                        ActivationEmailKind.WELCOME, ActivationEmailKind.WEEK_ONE_CHECK_IN);
    }

    private void post(String path, TestUser user) {
        ResponseEntity<Void> response =
                restTemplate.exchange(
                        path, HttpMethod.POST, new HttpEntity<>(user.authHeaders()), Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    private void setEmails(TestUser user, boolean enabled) {
        restTemplate.exchange(
                "/api/onboarding/activation-emails",
                HttpMethod.PUT,
                new HttpEntity<>(new ActivationEmailsRequest(enabled), user.authHeaders()),
                Void.class);
    }

    private Long createAccount(TestUser user) {
        return restTemplate
                .exchange(
                        "/api/accounts",
                        HttpMethod.POST,
                        new HttpEntity<>(
                                new AccountRequest(
                                        "Conta corrente", AccountType.CHECKING, BigDecimal.ZERO),
                                user.authHeaders()),
                        AccountResponse.class)
                .getBody()
                .id();
    }
}
