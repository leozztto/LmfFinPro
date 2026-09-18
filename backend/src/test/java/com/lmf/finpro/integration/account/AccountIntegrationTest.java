package com.lmf.finpro.integration.account;

import com.lmf.finpro.domain.model.AccountType;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.infrastructure.web.dto.account.AccountRequest;
import com.lmf.finpro.infrastructure.web.dto.account.AccountResponse;
import com.lmf.finpro.infrastructure.web.dto.transaction.TransactionRequest;
import com.lmf.finpro.infrastructure.web.dto.transaction.TransactionResponse;
import com.lmf.finpro.infrastructure.web.exception.ApiError;
import com.lmf.finpro.integration.support.AbstractIntegrationTest;
import com.lmf.finpro.integration.support.TestDataFactory;
import com.lmf.finpro.integration.support.TestUser;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AccountIntegrationTest extends AbstractIntegrationTest {

    @Test
    void createsListsUpdatesAndDeletesOwnAccount() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);

        AccountRequest createRequest = new AccountRequest("Conta corrente", AccountType.CHECKING, BigDecimal.valueOf(1000));
        ResponseEntity<AccountResponse> createResponse = restTemplate.exchange(
            "/api/accounts", HttpMethod.POST, new HttpEntity<>(createRequest, user.authHeaders()), AccountResponse.class
        );
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long accountId = createResponse.getBody().id();

        ResponseEntity<AccountResponse[]> listResponse = restTemplate.exchange(
            "/api/accounts", HttpMethod.GET, new HttpEntity<>(user.authHeaders()), AccountResponse[].class
        );
        assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(List.of(listResponse.getBody())).extracting(AccountResponse::id).contains(accountId);

        AccountRequest updateRequest = new AccountRequest("Conta corrente renomeada", AccountType.CHECKING, BigDecimal.valueOf(1500));
        ResponseEntity<AccountResponse> updateResponse = restTemplate.exchange(
            "/api/accounts/" + accountId, HttpMethod.PUT, new HttpEntity<>(updateRequest, user.authHeaders()), AccountResponse.class
        );
        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updateResponse.getBody().name()).isEqualTo("Conta corrente renomeada");

        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
            "/api/accounts/" + accountId, HttpMethod.DELETE, new HttpEntity<>(user.authHeaders()), Void.class
        );
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void userCannotAccessAnotherUsersAccount() {
        TestUser owner = TestDataFactory.registerRandomUser(restTemplate);
        TestUser intruder = TestDataFactory.registerRandomUser(restTemplate);

        AccountRequest createRequest = new AccountRequest("Conta da Ana", AccountType.WALLET, BigDecimal.ZERO);
        ResponseEntity<AccountResponse> createResponse = restTemplate.exchange(
            "/api/accounts", HttpMethod.POST, new HttpEntity<>(createRequest, owner.authHeaders()), AccountResponse.class
        );
        Long accountId = createResponse.getBody().id();

        ResponseEntity<ApiError> getResponse = restTemplate.exchange(
            "/api/accounts/" + accountId, HttpMethod.GET, new HttpEntity<>(intruder.authHeaders()), ApiError.class
        );

        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void currentBalanceStartsAtInitialBalanceAndFollowsTransactions() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);

        AccountRequest createRequest = new AccountRequest("Conta corrente", AccountType.CHECKING, BigDecimal.valueOf(1000));
        ResponseEntity<AccountResponse> createResponse = restTemplate.exchange(
            "/api/accounts", HttpMethod.POST, new HttpEntity<>(createRequest, user.authHeaders()), AccountResponse.class
        );
        Long accountId = createResponse.getBody().id();
        assertThat(createResponse.getBody().currentBalance()).isEqualByComparingTo(BigDecimal.valueOf(1000));

        createTransaction(user, accountId, BigDecimal.valueOf(500), CategoryType.INCOME);
        createTransaction(user, accountId, BigDecimal.valueOf(200), CategoryType.EXPENSE);

        ResponseEntity<AccountResponse> getResponse = restTemplate.exchange(
            "/api/accounts/" + accountId, HttpMethod.GET, new HttpEntity<>(user.authHeaders()), AccountResponse.class
        );
        assertThat(getResponse.getBody().currentBalance()).isEqualByComparingTo(BigDecimal.valueOf(1300));
    }

    private void createTransaction(TestUser user, Long accountId, BigDecimal amount, CategoryType type) {
        TransactionRequest request = new TransactionRequest(accountId, null, null, "Movimento", amount, LocalDate.now(), type);
        ResponseEntity<TransactionResponse> response = restTemplate.exchange(
            "/api/transactions", HttpMethod.POST, new HttpEntity<>(request, user.authHeaders()), TransactionResponse.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }
}
