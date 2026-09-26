package com.lmf.finpro.integration.transaction;

import static org.assertj.core.api.Assertions.assertThat;

import com.lmf.finpro.domain.model.AccountType;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.TransactionStatus;
import com.lmf.finpro.infrastructure.web.dto.account.AccountRequest;
import com.lmf.finpro.infrastructure.web.dto.account.AccountResponse;
import com.lmf.finpro.infrastructure.web.dto.dashboard.DashboardOverviewResponse;
import com.lmf.finpro.infrastructure.web.dto.transaction.TransactionRequest;
import com.lmf.finpro.infrastructure.web.dto.transaction.TransactionResponse;
import com.lmf.finpro.infrastructure.web.dto.transaction.TransactionStatusRequest;
import com.lmf.finpro.infrastructure.web.dto.transfer.TransferRequest;
import com.lmf.finpro.infrastructure.web.dto.transfer.TransferResponse;
import com.lmf.finpro.infrastructure.web.exception.ApiError;
import com.lmf.finpro.integration.support.AbstractIntegrationTest;
import com.lmf.finpro.integration.support.TestDataFactory;
import com.lmf.finpro.integration.support.TestUser;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;

class TransactionStatusIntegrationTest extends AbstractIntegrationTest {

    private static final LocalDate TODAY = LocalDate.now(ZoneId.of("America/Sao_Paulo"));

    private Long createAccount(TestUser user, BigDecimal initialBalance) {
        AccountRequest request =
                new AccountRequest("Conta corrente", AccountType.CHECKING, initialBalance);
        return restTemplate
                .exchange(
                        "/api/accounts",
                        HttpMethod.POST,
                        new HttpEntity<>(request, user.authHeaders()),
                        AccountResponse.class)
                .getBody()
                .id();
    }

    private TransactionResponse createTransaction(
            TestUser user,
            Long accountId,
            BigDecimal amount,
            LocalDate date,
            CategoryType type,
            TransactionStatus status) {
        TransactionRequest request =
                new TransactionRequest(
                        accountId, null, null, "Lançamento", amount, date, type, status);
        return restTemplate
                .exchange(
                        "/api/transactions",
                        HttpMethod.POST,
                        new HttpEntity<>(request, user.authHeaders()),
                        TransactionResponse.class)
                .getBody();
    }

    private BigDecimal currentBalance(TestUser user, Long accountId) {
        return restTemplate
                .exchange(
                        "/api/accounts/" + accountId,
                        HttpMethod.GET,
                        new HttpEntity<>(user.authHeaders()),
                        AccountResponse.class)
                .getBody()
                .currentBalance();
    }

    private <T> ResponseEntity<T> patchStatus(
            TestUser user, Long transactionId, TransactionStatus status, Class<T> responseType) {
        return restTemplate.exchange(
                "/api/transactions/" + transactionId + "/status",
                HttpMethod.PATCH,
                new HttpEntity<>(new TransactionStatusRequest(status), user.authHeaders()),
                responseType);
    }

    @Test
    void statusDefaultsByDateWhenNotInformed() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        Long accountId = createAccount(user, BigDecimal.ZERO);

        TransactionResponse today =
                createTransaction(
                        user, accountId, BigDecimal.TEN, TODAY, CategoryType.EXPENSE, null);
        TransactionResponse future =
                createTransaction(
                        user,
                        accountId,
                        BigDecimal.TEN,
                        TODAY.plusDays(10),
                        CategoryType.EXPENSE,
                        null);

        assertThat(today.status()).isEqualTo(TransactionStatus.PAID);
        assertThat(future.status()).isEqualTo(TransactionStatus.PENDING);
    }

    @Test
    void pendingTransactionOnlyAffectsBalanceOnceMarkedAsPaid() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        Long accountId = createAccount(user, BigDecimal.valueOf(1000));

        TransactionResponse invoice =
                createTransaction(
                        user,
                        accountId,
                        BigDecimal.valueOf(400),
                        TODAY,
                        CategoryType.INCOME,
                        TransactionStatus.PENDING);
        assertThat(invoice.status()).isEqualTo(TransactionStatus.PENDING);
        assertThat(currentBalance(user, accountId)).isEqualByComparingTo("1000");

        ResponseEntity<TransactionResponse> paidResponse =
                patchStatus(user, invoice.id(), TransactionStatus.PAID, TransactionResponse.class);

        assertThat(paidResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(paidResponse.getBody().status()).isEqualTo(TransactionStatus.PAID);
        assertThat(currentBalance(user, accountId)).isEqualByComparingTo("1400");
    }

    @Test
    void dashboardSeparatesCurrentAndProjectedBalance() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        Long accountId = createAccount(user, BigDecimal.valueOf(1000));
        createTransaction(
                user,
                accountId,
                BigDecimal.valueOf(300),
                TODAY,
                CategoryType.INCOME,
                TransactionStatus.PENDING);
        createTransaction(
                user,
                accountId,
                BigDecimal.valueOf(100),
                TODAY,
                CategoryType.EXPENSE,
                TransactionStatus.PENDING);
        createTransaction(
                user,
                accountId,
                BigDecimal.valueOf(50),
                TODAY,
                CategoryType.EXPENSE,
                TransactionStatus.PAID);

        DashboardOverviewResponse overview =
                restTemplate
                        .exchange(
                                "/api/dashboard/overview",
                                HttpMethod.GET,
                                new HttpEntity<>(user.authHeaders()),
                                DashboardOverviewResponse.class)
                        .getBody();

        assertThat(overview.currentBalance()).isEqualByComparingTo("950");
        assertThat(overview.pendingIncome()).isEqualByComparingTo("300");
        assertThat(overview.pendingExpense()).isEqualByComparingTo("100");
        assertThat(overview.projectedBalance()).isEqualByComparingTo("1150");
    }

    @Test
    void transferTransactionsCannotBeMarkedAsPending() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        Long fromAccountId = createAccount(user, BigDecimal.valueOf(500));
        Long toAccountId = createAccount(user, BigDecimal.ZERO);
        TransferResponse transfer =
                restTemplate
                        .exchange(
                                "/api/transfers",
                                HttpMethod.POST,
                                new HttpEntity<>(
                                        new TransferRequest(
                                                fromAccountId,
                                                toAccountId,
                                                BigDecimal.valueOf(100),
                                                TODAY,
                                                null),
                                        user.authHeaders()),
                                TransferResponse.class)
                        .getBody();

        ResponseEntity<ApiError> response =
                patchStatus(
                        user,
                        transfer.fromTransactionId(),
                        TransactionStatus.PENDING,
                        ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void userCannotChangeStatusOfAnotherUsersTransaction() {
        TestUser owner = TestDataFactory.registerRandomUser(restTemplate);
        TestUser intruder = TestDataFactory.registerRandomUser(restTemplate);
        Long accountId = createAccount(owner, BigDecimal.ZERO);
        TransactionResponse transaction =
                createTransaction(
                        owner, accountId, BigDecimal.TEN, TODAY, CategoryType.EXPENSE, null);

        ResponseEntity<ApiError> response =
                patchStatus(intruder, transaction.id(), TransactionStatus.PENDING, ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
