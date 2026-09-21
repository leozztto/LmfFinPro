package com.lmf.finpro.integration.transfer;

import static org.assertj.core.api.Assertions.assertThat;

import com.lmf.finpro.domain.model.AccountType;
import com.lmf.finpro.infrastructure.web.dto.account.AccountRequest;
import com.lmf.finpro.infrastructure.web.dto.account.AccountResponse;
import com.lmf.finpro.infrastructure.web.dto.transaction.TransactionResponse;
import com.lmf.finpro.infrastructure.web.dto.transfer.TransferRequest;
import com.lmf.finpro.infrastructure.web.dto.transfer.TransferResponse;
import com.lmf.finpro.infrastructure.web.exception.ApiError;
import com.lmf.finpro.integration.support.AbstractIntegrationTest;
import com.lmf.finpro.integration.support.TestDataFactory;
import com.lmf.finpro.integration.support.TestUser;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;

class TransferIntegrationTest extends AbstractIntegrationTest {

    @Test
    void listReturnsEmptyForUserWithNoTransfers() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);

        ResponseEntity<TransferResponse[]> response =
                restTemplate.exchange(
                        "/api/transfers",
                        HttpMethod.GET,
                        new HttpEntity<>(user.authHeaders()),
                        TransferResponse[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void createsListsAndDeletesTransferBetweenOwnAccounts() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        Long fromAccountId = createAccount(user, "Conta Corrente", BigDecimal.valueOf(1000));
        Long toAccountId = createAccount(user, "Poupança");

        TransferRequest createRequest =
                new TransferRequest(
                        fromAccountId, toAccountId, BigDecimal.valueOf(500), LocalDate.now(), null);
        ResponseEntity<TransferResponse> createResponse =
                restTemplate.exchange(
                        "/api/transfers",
                        HttpMethod.POST,
                        new HttpEntity<>(createRequest, user.authHeaders()),
                        TransferResponse.class);
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        TransferResponse created = createResponse.getBody();
        assertThat(created.fromTransactionId()).isNotNull();
        assertThat(created.toTransactionId()).isNotNull();

        ResponseEntity<TransferResponse[]> listResponse =
                restTemplate.exchange(
                        "/api/transfers",
                        HttpMethod.GET,
                        new HttpEntity<>(user.authHeaders()),
                        TransferResponse[].class);
        assertThat(List.of(listResponse.getBody()))
                .extracting(TransferResponse::id)
                .contains(created.id());

        ResponseEntity<TransactionResponse[]> transactionsAfterCreate =
                restTemplate.exchange(
                        "/api/transactions",
                        HttpMethod.GET,
                        new HttpEntity<>(user.authHeaders()),
                        TransactionResponse[].class);
        assertThat(List.of(transactionsAfterCreate.getBody()))
                .extracting(TransactionResponse::id)
                .contains(created.fromTransactionId(), created.toTransactionId());
        assertThat(List.of(transactionsAfterCreate.getBody()))
                .filteredOn(
                        t ->
                                t.id().equals(created.fromTransactionId())
                                        || t.id().equals(created.toTransactionId()))
                .extracting(TransactionResponse::transferId)
                .containsOnly(created.id());

        ResponseEntity<Void> deleteResponse =
                restTemplate.exchange(
                        "/api/transfers/" + created.id(),
                        HttpMethod.DELETE,
                        new HttpEntity<>(user.authHeaders()),
                        Void.class);
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<TransactionResponse[]> transactionsAfterDelete =
                restTemplate.exchange(
                        "/api/transactions",
                        HttpMethod.GET,
                        new HttpEntity<>(user.authHeaders()),
                        TransactionResponse[].class);
        assertThat(List.of(transactionsAfterDelete.getBody()))
                .extracting(TransactionResponse::id)
                .doesNotContain(created.fromTransactionId(), created.toTransactionId());
    }

    @Test
    void rejectsTransferToSameAccount() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        Long accountId = createAccount(user, "Conta Única");

        TransferRequest createRequest =
                new TransferRequest(
                        accountId, accountId, BigDecimal.valueOf(100), LocalDate.now(), null);
        ResponseEntity<ApiError> response =
                restTemplate.exchange(
                        "/api/transfers",
                        HttpMethod.POST,
                        new HttpEntity<>(createRequest, user.authHeaders()),
                        ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void rejectsTransferWithInsufficientBalance() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        Long fromAccountId = createAccount(user, "Conta Sem Saldo");
        Long toAccountId = createAccount(user, "Poupança");

        TransferRequest createRequest =
                new TransferRequest(
                        fromAccountId, toAccountId, BigDecimal.valueOf(100), LocalDate.now(), null);
        ResponseEntity<ApiError> response =
                restTemplate.exchange(
                        "/api/transfers",
                        HttpMethod.POST,
                        new HttpEntity<>(createRequest, user.authHeaders()),
                        ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void rejectsTransferUsingAnotherUsersAccount() {
        TestUser owner = TestDataFactory.registerRandomUser(restTemplate);
        TestUser intruder = TestDataFactory.registerRandomUser(restTemplate);
        Long ownerAccountId = createAccount(owner, "Conta do Dono");
        Long intruderAccountId = createAccount(intruder, "Conta do Intruso");

        TransferRequest createRequest =
                new TransferRequest(
                        intruderAccountId,
                        ownerAccountId,
                        BigDecimal.valueOf(100),
                        LocalDate.now(),
                        null);
        ResponseEntity<ApiError> response =
                restTemplate.exchange(
                        "/api/transfers",
                        HttpMethod.POST,
                        new HttpEntity<>(createRequest, intruder.authHeaders()),
                        ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void blocksDeletingIndividualTransferLeg() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        Long fromAccountId = createAccount(user, "Conta Corrente", BigDecimal.valueOf(1000));
        Long toAccountId = createAccount(user, "Poupança");

        TransferRequest createRequest =
                new TransferRequest(
                        fromAccountId, toAccountId, BigDecimal.valueOf(200), LocalDate.now(), null);
        ResponseEntity<TransferResponse> createResponse =
                restTemplate.exchange(
                        "/api/transfers",
                        HttpMethod.POST,
                        new HttpEntity<>(createRequest, user.authHeaders()),
                        TransferResponse.class);
        Long fromTransactionId = createResponse.getBody().fromTransactionId();

        ResponseEntity<ApiError> deleteResponse =
                restTemplate.exchange(
                        "/api/transactions/" + fromTransactionId,
                        HttpMethod.DELETE,
                        new HttpEntity<>(user.authHeaders()),
                        ApiError.class);

        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private Long createAccount(TestUser user, String name) {
        return createAccount(user, name, BigDecimal.ZERO);
    }

    private Long createAccount(TestUser user, String name, BigDecimal initialBalance) {
        AccountRequest accountRequest =
                new AccountRequest(name, AccountType.CHECKING, initialBalance);
        ResponseEntity<AccountResponse> response =
                restTemplate.exchange(
                        "/api/accounts",
                        HttpMethod.POST,
                        new HttpEntity<>(accountRequest, user.authHeaders()),
                        AccountResponse.class);
        return response.getBody().id();
    }
}
