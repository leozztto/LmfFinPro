package com.lmf.finpro.integration.category;

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

class CategoryIntegrationTest extends AbstractIntegrationTest {

    @Test
    void createsListsAndUpdatesOwnCategory() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);

        CategoryRequest createRequest = new CategoryRequest("Consultoria", CategoryType.INCOME, "#2E6E4E", "briefcase");
        ResponseEntity<CategoryResponse> createResponse = restTemplate.exchange(
            "/api/categories", HttpMethod.POST, new HttpEntity<>(createRequest, user.authHeaders()), CategoryResponse.class
        );
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResponse.getBody().global()).isFalse();
        Long categoryId = createResponse.getBody().id();

        ResponseEntity<CategoryResponse[]> listResponse = restTemplate.exchange(
            "/api/categories", HttpMethod.GET, new HttpEntity<>(user.authHeaders()), CategoryResponse[].class
        );
        assertThat(List.of(listResponse.getBody())).extracting(CategoryResponse::id).contains(categoryId);

        CategoryRequest updateRequest = new CategoryRequest("Consultoria PJ", CategoryType.INCOME, "#2E6E4E", "briefcase");
        ResponseEntity<CategoryResponse> updateResponse = restTemplate.exchange(
            "/api/categories/" + categoryId, HttpMethod.PUT, new HttpEntity<>(updateRequest, user.authHeaders()), CategoryResponse.class
        );
        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updateResponse.getBody().name()).isEqualTo("Consultoria PJ");
    }

    @Test
    void categoryTypeCannotBeChangedOnUpdate() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);

        CategoryRequest createRequest = new CategoryRequest("Freelas", CategoryType.EXPENSE, null, null);
        ResponseEntity<CategoryResponse> createResponse = restTemplate.exchange(
            "/api/categories", HttpMethod.POST, new HttpEntity<>(createRequest, user.authHeaders()), CategoryResponse.class
        );
        Long categoryId = createResponse.getBody().id();

        // Tenta trocar o tipo na edição: deve ser ignorado — transações já lançadas dependem do tipo original.
        CategoryRequest updateRequest = new CategoryRequest("Freelas", CategoryType.INCOME, null, null);
        ResponseEntity<CategoryResponse> updateResponse = restTemplate.exchange(
            "/api/categories/" + categoryId, HttpMethod.PUT, new HttpEntity<>(updateRequest, user.authHeaders()), CategoryResponse.class
        );

        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updateResponse.getBody().type()).isEqualTo(CategoryType.EXPENSE);
    }

    @Test
    void userCannotEditAnotherUsersCategory() {
        TestUser owner = TestDataFactory.registerRandomUser(restTemplate);
        TestUser intruder = TestDataFactory.registerRandomUser(restTemplate);

        CategoryRequest createRequest = new CategoryRequest("Categoria da Ana", CategoryType.EXPENSE, null, null);
        ResponseEntity<CategoryResponse> createResponse = restTemplate.exchange(
            "/api/categories", HttpMethod.POST, new HttpEntity<>(createRequest, owner.authHeaders()), CategoryResponse.class
        );
        Long categoryId = createResponse.getBody().id();

        ResponseEntity<ApiError> updateResponse = restTemplate.exchange(
            "/api/categories/" + categoryId, HttpMethod.PUT,
            new HttpEntity<>(createRequest, intruder.authHeaders()), ApiError.class
        );

        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void userCannotDeleteCategoryWithLinkedTransaction() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);

        CategoryRequest categoryRequest = new CategoryRequest("Moradia", CategoryType.EXPENSE, null, null);
        ResponseEntity<CategoryResponse> categoryResponse = restTemplate.exchange(
            "/api/categories", HttpMethod.POST, new HttpEntity<>(categoryRequest, user.authHeaders()), CategoryResponse.class
        );
        Long categoryId = categoryResponse.getBody().id();

        AccountRequest accountRequest = new AccountRequest("Conta", AccountType.CHECKING, BigDecimal.ZERO);
        ResponseEntity<AccountResponse> accountResponse = restTemplate.exchange(
            "/api/accounts", HttpMethod.POST, new HttpEntity<>(accountRequest, user.authHeaders()), AccountResponse.class
        );
        Long accountId = accountResponse.getBody().id();

        TransactionRequest transactionRequest = new TransactionRequest(
            accountId, categoryId, null, "Aluguel", BigDecimal.valueOf(1500), LocalDate.now(), CategoryType.EXPENSE
        );
        restTemplate.exchange(
            "/api/transactions", HttpMethod.POST, new HttpEntity<>(transactionRequest, user.authHeaders()), TransactionResponse.class
        );

        ResponseEntity<ApiError> deleteResponse = restTemplate.exchange(
            "/api/categories/" + categoryId, HttpMethod.DELETE, new HttpEntity<>(user.authHeaders()), ApiError.class
        );

        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }
}
