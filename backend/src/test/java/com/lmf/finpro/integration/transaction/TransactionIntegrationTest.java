package com.lmf.finpro.integration.transaction;

import com.lmf.finpro.domain.model.AccountType;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.infrastructure.web.dto.account.AccountRequest;
import com.lmf.finpro.infrastructure.web.dto.account.AccountResponse;
import com.lmf.finpro.infrastructure.web.dto.category.CategoryRequest;
import com.lmf.finpro.infrastructure.web.dto.category.CategoryResponse;
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

class TransactionIntegrationTest extends AbstractIntegrationTest {

    @Test
    void createsListsUpdatesAndDeletesTransactionOnOwnAccount() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        Long accountId = createAccount(user);

        TransactionRequest createRequest = new TransactionRequest(
            accountId, null, null, "Pagamento cliente X", BigDecimal.valueOf(2500), LocalDate.now(), CategoryType.INCOME
        );
        ResponseEntity<TransactionResponse> createResponse = restTemplate.exchange(
            "/api/transactions", HttpMethod.POST, new HttpEntity<>(createRequest, user.authHeaders()), TransactionResponse.class
        );
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long transactionId = createResponse.getBody().id();

        ResponseEntity<TransactionResponse[]> listResponse = restTemplate.exchange(
            "/api/transactions", HttpMethod.GET, new HttpEntity<>(user.authHeaders()), TransactionResponse[].class
        );
        assertThat(List.of(listResponse.getBody())).extracting(TransactionResponse::id).contains(transactionId);

        TransactionRequest updateRequest = new TransactionRequest(
            accountId, null, null, "Pagamento cliente X (ajustado)", BigDecimal.valueOf(2600), LocalDate.now(), CategoryType.INCOME
        );
        ResponseEntity<TransactionResponse> updateResponse = restTemplate.exchange(
            "/api/transactions/" + transactionId, HttpMethod.PUT,
            new HttpEntity<>(updateRequest, user.authHeaders()), TransactionResponse.class
        );
        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updateResponse.getBody().amount()).isEqualByComparingTo(BigDecimal.valueOf(2600));

        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
            "/api/transactions/" + transactionId, HttpMethod.DELETE, new HttpEntity<>(user.authHeaders()), Void.class
        );
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void userCannotCreateTransactionOnAnotherUsersAccount() {
        TestUser owner = TestDataFactory.registerRandomUser(restTemplate);
        TestUser intruder = TestDataFactory.registerRandomUser(restTemplate);
        Long ownerAccountId = createAccount(owner);

        TransactionRequest createRequest = new TransactionRequest(
            ownerAccountId, null, null, "Tentativa indevida", BigDecimal.TEN, LocalDate.now(), CategoryType.EXPENSE
        );

        ResponseEntity<ApiError> response = restTemplate.exchange(
            "/api/transactions", HttpMethod.POST, new HttpEntity<>(createRequest, intruder.authHeaders()), ApiError.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void rejectsTransactionWithNegativeOrZeroAmount() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        Long accountId = createAccount(user);

        TransactionRequest negativeRequest = new TransactionRequest(
            accountId, null, null, "Despesa com valor negativo", BigDecimal.valueOf(-500), LocalDate.now(), CategoryType.EXPENSE
        );
        ResponseEntity<ApiError> negativeResponse = restTemplate.exchange(
            "/api/transactions", HttpMethod.POST, new HttpEntity<>(negativeRequest, user.authHeaders()), ApiError.class
        );
        assertThat(negativeResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        TransactionRequest zeroRequest = new TransactionRequest(
            accountId, null, null, "Despesa com valor zero", BigDecimal.ZERO, LocalDate.now(), CategoryType.EXPENSE
        );
        ResponseEntity<ApiError> zeroResponse = restTemplate.exchange(
            "/api/transactions", HttpMethod.POST, new HttpEntity<>(zeroRequest, user.authHeaders()), ApiError.class
        );
        assertThat(zeroResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void userCannotCreateExpenseTransactionWithIncomeCategory() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        Long accountId = createAccount(user);
        Long incomeCategoryId = createCategory(user, CategoryType.INCOME);

        TransactionRequest createRequest = new TransactionRequest(
            accountId, incomeCategoryId, null, "Despesa com categoria errada", BigDecimal.TEN, LocalDate.now(), CategoryType.EXPENSE
        );

        ResponseEntity<ApiError> response = restTemplate.exchange(
            "/api/transactions", HttpMethod.POST, new HttpEntity<>(createRequest, user.authHeaders()), ApiError.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private Long createCategory(TestUser user, CategoryType type) {
        CategoryRequest categoryRequest = new CategoryRequest("Categoria " + type, type, null, null);
        ResponseEntity<CategoryResponse> response = restTemplate.exchange(
            "/api/categories", HttpMethod.POST, new HttpEntity<>(categoryRequest, user.authHeaders()), CategoryResponse.class
        );
        return response.getBody().id();
    }

    private Long createAccount(TestUser user) {
        AccountRequest accountRequest = new AccountRequest("Conta para transações", AccountType.CHECKING, BigDecimal.ZERO);
        ResponseEntity<AccountResponse> response = restTemplate.exchange(
            "/api/accounts", HttpMethod.POST, new HttpEntity<>(accountRequest, user.authHeaders()), AccountResponse.class
        );
        return response.getBody().id();
    }
}
