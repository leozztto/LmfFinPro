package com.lmf.finpro.integration.recurringtransaction;

import static org.assertj.core.api.Assertions.assertThat;

import com.lmf.finpro.domain.model.AccountType;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.RecurrenceFrequency;
import com.lmf.finpro.domain.model.TransactionOrigin;
import com.lmf.finpro.infrastructure.web.dto.account.AccountRequest;
import com.lmf.finpro.infrastructure.web.dto.account.AccountResponse;
import com.lmf.finpro.infrastructure.web.dto.recurringtransaction.RecurringTransactionRequest;
import com.lmf.finpro.infrastructure.web.dto.recurringtransaction.RecurringTransactionResponse;
import com.lmf.finpro.infrastructure.web.dto.recurringtransaction.RecurringTransactionUpdateRequest;
import com.lmf.finpro.infrastructure.web.dto.transaction.TransactionResponse;
import com.lmf.finpro.infrastructure.web.exception.ApiError;
import com.lmf.finpro.integration.support.AbstractIntegrationTest;
import com.lmf.finpro.integration.support.TestDataFactory;
import com.lmf.finpro.integration.support.TestUser;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;

class RecurringTransactionIntegrationTest extends AbstractIntegrationTest {

    private static final LocalDate TODAY = LocalDate.now(ZoneId.of("America/Sao_Paulo"));

    private Long createAccount(TestUser user) {
        AccountRequest accountRequest =
                new AccountRequest("Conta corrente", AccountType.CHECKING, BigDecimal.ZERO);
        return restTemplate
                .exchange(
                        "/api/accounts",
                        HttpMethod.POST,
                        new HttpEntity<>(accountRequest, user.authHeaders()),
                        AccountResponse.class)
                .getBody()
                .id();
    }

    private ResponseEntity<RecurringTransactionResponse> createRecurrence(
            TestUser user, Long accountId, LocalDate startDate) {
        RecurringTransactionRequest request =
                new RecurringTransactionRequest(
                        accountId,
                        null,
                        null,
                        "Aluguel",
                        BigDecimal.valueOf(1500),
                        CategoryType.EXPENSE,
                        RecurrenceFrequency.MONTHLY,
                        startDate,
                        null);
        return restTemplate.exchange(
                "/api/recurring-transactions",
                HttpMethod.POST,
                new HttpEntity<>(request, user.authHeaders()),
                RecurringTransactionResponse.class);
    }

    private List<TransactionResponse> listTransactions(TestUser user) {
        return List.of(
                restTemplate
                        .exchange(
                                "/api/transactions",
                                HttpMethod.GET,
                                new HttpEntity<>(user.authHeaders()),
                                TransactionResponse[].class)
                        .getBody());
    }

    @Test
    void creatingWithPastStartDateLaunchesMissingOccurrencesAsTransactions() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        Long accountId = createAccount(user);

        ResponseEntity<RecurringTransactionResponse> response =
                createRecurrence(user, accountId, TODAY.minusMonths(2));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        RecurringTransactionResponse created = response.getBody();
        assertThat(created.generatedOccurrences()).isEqualTo(3);
        assertThat(created.active()).isTrue();
        assertThat(created.nextOccurrenceDate()).isAfter(TODAY);

        List<TransactionResponse> transactions = listTransactions(user);
        assertThat(transactions).hasSize(3);
        assertThat(transactions)
                .allSatisfy(
                        transaction -> {
                            assertThat(transaction.origin()).isEqualTo(TransactionOrigin.RECURRING);
                            assertThat(transaction.recurringTransactionId())
                                    .isEqualTo(created.id());
                            assertThat(transaction.amount()).isEqualByComparingTo("1500");
                        });
    }

    @Test
    void creatingWithFutureStartDateDoesNotLaunchAnythingYet() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        Long accountId = createAccount(user);

        RecurringTransactionResponse created =
                createRecurrence(user, accountId, TODAY.plusDays(10)).getBody();

        assertThat(created.generatedOccurrences()).isZero();
        assertThat(created.nextOccurrenceDate()).isEqualTo(TODAY.plusDays(10));
        assertThat(listTransactions(user)).isEmpty();
    }

    @Test
    void updatesPausesAndDeletesRecurrenceKeepingLaunchedTransactions() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        Long accountId = createAccount(user);
        Long recurrenceId = createRecurrence(user, accountId, TODAY).getBody().id();

        RecurringTransactionUpdateRequest pause =
                new RecurringTransactionUpdateRequest(
                        null, null, "Aluguel reajustado", BigDecimal.valueOf(1650), null, false);
        ResponseEntity<RecurringTransactionResponse> updateResponse =
                restTemplate.exchange(
                        "/api/recurring-transactions/" + recurrenceId,
                        HttpMethod.PUT,
                        new HttpEntity<>(pause, user.authHeaders()),
                        RecurringTransactionResponse.class);
        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updateResponse.getBody().description()).isEqualTo("Aluguel reajustado");
        assertThat(updateResponse.getBody().active()).isFalse();
        assertThat(updateResponse.getBody().nextOccurrenceDate()).isNull();

        ResponseEntity<Void> deleteResponse =
                restTemplate.exchange(
                        "/api/recurring-transactions/" + recurrenceId,
                        HttpMethod.DELETE,
                        new HttpEntity<>(user.authHeaders()),
                        Void.class);
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        List<TransactionResponse> transactions = listTransactions(user);
        assertThat(transactions).hasSize(1);
        assertThat(transactions.get(0).recurringTransactionId()).isNull();
        assertThat(transactions.get(0).origin()).isEqualTo(TransactionOrigin.RECURRING);
    }

    @Test
    void rejectsEndDateBeforeStartDate() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        Long accountId = createAccount(user);
        RecurringTransactionRequest request =
                new RecurringTransactionRequest(
                        accountId,
                        null,
                        null,
                        "Aluguel",
                        BigDecimal.valueOf(1500),
                        CategoryType.EXPENSE,
                        RecurrenceFrequency.MONTHLY,
                        TODAY,
                        TODAY.minusDays(1));

        ResponseEntity<ApiError> response =
                restTemplate.exchange(
                        "/api/recurring-transactions",
                        HttpMethod.POST,
                        new HttpEntity<>(request, user.authHeaders()),
                        ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void accountWithRecurrenceCannotBeDeleted() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        Long accountId = createAccount(user);
        createRecurrence(user, accountId, TODAY.plusMonths(1));

        ResponseEntity<ApiError> response =
                restTemplate.exchange(
                        "/api/accounts/" + accountId,
                        HttpMethod.DELETE,
                        new HttpEntity<>(user.authHeaders()),
                        ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void userCannotAccessAnotherUsersRecurrence() {
        TestUser owner = TestDataFactory.registerRandomUser(restTemplate);
        TestUser intruder = TestDataFactory.registerRandomUser(restTemplate);
        Long accountId = createAccount(owner);
        Long recurrenceId = createRecurrence(owner, accountId, TODAY.plusDays(5)).getBody().id();

        ResponseEntity<ApiError> deleteResponse =
                restTemplate.exchange(
                        "/api/recurring-transactions/" + recurrenceId,
                        HttpMethod.DELETE,
                        new HttpEntity<>(intruder.authHeaders()),
                        ApiError.class);
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        ResponseEntity<ApiError> createOnForeignAccount =
                restTemplate.exchange(
                        "/api/recurring-transactions",
                        HttpMethod.POST,
                        new HttpEntity<>(
                                new RecurringTransactionRequest(
                                        accountId,
                                        null,
                                        null,
                                        "Invasão",
                                        BigDecimal.TEN,
                                        CategoryType.EXPENSE,
                                        RecurrenceFrequency.WEEKLY,
                                        TODAY,
                                        null),
                                intruder.authHeaders()),
                        ApiError.class);
        assertThat(createOnForeignAccount.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
