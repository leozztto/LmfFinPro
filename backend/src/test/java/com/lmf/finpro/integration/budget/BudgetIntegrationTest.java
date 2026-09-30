package com.lmf.finpro.integration.budget;

import static org.assertj.core.api.Assertions.assertThat;

import com.lmf.finpro.domain.model.AccountType;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.ClientWorkType;
import com.lmf.finpro.domain.model.DocumentType;
import com.lmf.finpro.infrastructure.web.dto.account.AccountRequest;
import com.lmf.finpro.infrastructure.web.dto.account.AccountResponse;
import com.lmf.finpro.infrastructure.web.dto.budget.BudgetRequest;
import com.lmf.finpro.infrastructure.web.dto.budget.BudgetResponse;
import com.lmf.finpro.infrastructure.web.dto.category.CategoryRequest;
import com.lmf.finpro.infrastructure.web.dto.category.CategoryResponse;
import com.lmf.finpro.infrastructure.web.dto.client.ClientRequest;
import com.lmf.finpro.infrastructure.web.dto.client.ClientResponse;
import com.lmf.finpro.infrastructure.web.dto.transaction.TransactionRequest;
import com.lmf.finpro.infrastructure.web.dto.transaction.TransactionResponse;
import com.lmf.finpro.infrastructure.web.exception.ApiError;
import com.lmf.finpro.integration.support.AbstractIntegrationTest;
import com.lmf.finpro.integration.support.CnpjTestFactory;
import com.lmf.finpro.integration.support.TestDataFactory;
import com.lmf.finpro.integration.support.TestUser;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;

class BudgetIntegrationTest extends AbstractIntegrationTest {

    private Long createCategory(TestUser user) {
        CategoryRequest categoryRequest =
                new CategoryRequest("Alimentação", CategoryType.EXPENSE, null, null);
        ResponseEntity<CategoryResponse> response =
                restTemplate.exchange(
                        "/api/categories",
                        HttpMethod.POST,
                        new HttpEntity<>(categoryRequest, user.authHeaders()),
                        CategoryResponse.class);
        return response.getBody().id();
    }

    private Long createAccount(TestUser user) {
        AccountRequest accountRequest =
                new AccountRequest("Carteira", AccountType.CHECKING, BigDecimal.valueOf(1000));
        ResponseEntity<AccountResponse> response =
                restTemplate.exchange(
                        "/api/accounts",
                        HttpMethod.POST,
                        new HttpEntity<>(accountRequest, user.authHeaders()),
                        AccountResponse.class);
        return response.getBody().id();
    }

    private void createExpense(
            TestUser user, Long accountId, Long categoryId, BigDecimal amount, LocalDate date) {
        TransactionRequest transactionRequest =
                new TransactionRequest(
                        accountId,
                        categoryId,
                        null,
                        "Despesa de teste",
                        amount,
                        date,
                        CategoryType.EXPENSE);
        restTemplate.exchange(
                "/api/transactions",
                HttpMethod.POST,
                new HttpEntity<>(transactionRequest, user.authHeaders()),
                TransactionResponse.class);
    }

    @Test
    void createsListsAndDeletesOwnBudget() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        Long categoryId = createCategory(user);

        BudgetRequest createRequest =
                new BudgetRequest(categoryId, YearMonth.now(), BigDecimal.valueOf(500), null);
        ResponseEntity<BudgetResponse> createResponse =
                restTemplate.exchange(
                        "/api/budgets",
                        HttpMethod.POST,
                        new HttpEntity<>(createRequest, user.authHeaders()),
                        BudgetResponse.class);
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResponse.getBody().limitValue()).isEqualByComparingTo("500");
        assertThat(createResponse.getBody().categoryId()).isEqualTo(categoryId);
        assertThat(createResponse.getBody().spentValue()).isEqualByComparingTo("0");
        Long budgetId = createResponse.getBody().id();

        ResponseEntity<BudgetResponse[]> listResponse =
                restTemplate.exchange(
                        "/api/budgets",
                        HttpMethod.GET,
                        new HttpEntity<>(user.authHeaders()),
                        BudgetResponse[].class);
        assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(List.of(listResponse.getBody()))
                .extracting(BudgetResponse::id)
                .contains(budgetId);

        ResponseEntity<Void> deleteResponse =
                restTemplate.exchange(
                        "/api/budgets/" + budgetId,
                        HttpMethod.DELETE,
                        new HttpEntity<>(user.authHeaders()),
                        Void.class);
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void calculatesSpentValueFromExpensesInTheSameCategoryAndMonth() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        Long categoryId = createCategory(user);
        Long otherCategoryId = createCategory(user);
        Long accountId = createAccount(user);
        YearMonth currentMonth = YearMonth.now();

        // conta para o orçamento: duas despesas na categoria/mês certos
        createExpense(user, accountId, categoryId, BigDecimal.valueOf(150), currentMonth.atDay(1));
        createExpense(user, accountId, categoryId, BigDecimal.valueOf(50), currentMonth.atDay(2));
        // não deve contar: outra categoria e mês passado
        createExpense(
                user, accountId, otherCategoryId, BigDecimal.valueOf(999), currentMonth.atDay(1));
        createExpense(
                user,
                accountId,
                categoryId,
                BigDecimal.valueOf(999),
                currentMonth.minusMonths(1).atDay(1));

        BudgetRequest createRequest =
                new BudgetRequest(categoryId, currentMonth, BigDecimal.valueOf(500), null);
        ResponseEntity<BudgetResponse> createResponse =
                restTemplate.exchange(
                        "/api/budgets",
                        HttpMethod.POST,
                        new HttpEntity<>(createRequest, user.authHeaders()),
                        BudgetResponse.class);

        assertThat(createResponse.getBody().spentValue()).isEqualByComparingTo("200");
    }

    @Test
    void rejectsBudgetWithZeroLimitValue() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        Long categoryId = createCategory(user);

        BudgetRequest createRequest =
                new BudgetRequest(categoryId, YearMonth.now(), BigDecimal.ZERO, null);
        ResponseEntity<ApiError> createResponse =
                restTemplate.exchange(
                        "/api/budgets",
                        HttpMethod.POST,
                        new HttpEntity<>(createRequest, user.authHeaders()),
                        ApiError.class);

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void userCannotAccessAnotherUsersBudget() {
        TestUser owner = TestDataFactory.registerRandomUser(restTemplate);
        TestUser intruder = TestDataFactory.registerRandomUser(restTemplate);
        Long categoryId = createCategory(owner);

        BudgetRequest createRequest =
                new BudgetRequest(categoryId, YearMonth.now(), BigDecimal.valueOf(300), null);
        ResponseEntity<BudgetResponse> createResponse =
                restTemplate.exchange(
                        "/api/budgets",
                        HttpMethod.POST,
                        new HttpEntity<>(createRequest, owner.authHeaders()),
                        BudgetResponse.class);
        Long budgetId = createResponse.getBody().id();

        ResponseEntity<ApiError> deleteResponse =
                restTemplate.exchange(
                        "/api/budgets/" + budgetId,
                        HttpMethod.DELETE,
                        new HttpEntity<>(intruder.authHeaders()),
                        ApiError.class);

        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private Long createClient(TestUser user) {
        ClientRequest request =
                new ClientRequest(
                        "Acme Ltda",
                        "contato@acme.com",
                        "11999998888",
                        DocumentType.CNPJ,
                        CnpjTestFactory.randomValidCnpj(),
                        ClientWorkType.PJ,
                        null,
                        null,
                        true);
        return restTemplate
                .exchange(
                        "/api/clients",
                        HttpMethod.POST,
                        new HttpEntity<>(request, user.authHeaders()),
                        ClientResponse.class)
                .getBody()
                .id();
    }

    private void createClientExpense(
            TestUser user,
            Long accountId,
            Long categoryId,
            Long clientId,
            BigDecimal amount,
            LocalDate date) {
        TransactionRequest request =
                new TransactionRequest(
                        accountId,
                        categoryId,
                        clientId,
                        "Despesa do projeto",
                        amount,
                        date,
                        CategoryType.EXPENSE);
        restTemplate.exchange(
                "/api/transactions",
                HttpMethod.POST,
                new HttpEntity<>(request, user.authHeaders()),
                TransactionResponse.class);
    }

    @Test
    void clientBudgetCountsOnlyExpensesOfThatClient() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        Long categoryId = createCategory(user);
        Long accountId = createAccount(user);
        Long clientId = createClient(user);
        Long otherClientId = createClient(user);
        YearMonth currentMonth = YearMonth.now();

        createClientExpense(
                user,
                accountId,
                categoryId,
                clientId,
                BigDecimal.valueOf(120),
                currentMonth.atDay(1));
        createClientExpense(
                user,
                accountId,
                categoryId,
                otherClientId,
                BigDecimal.valueOf(300),
                currentMonth.atDay(1));
        createExpense(user, accountId, categoryId, BigDecimal.valueOf(80), currentMonth.atDay(2));

        BudgetResponse clientBudget =
                createBudget(user, categoryId, currentMonth, BigDecimal.valueOf(500), clientId)
                        .getBody();
        BudgetResponse generalBudget =
                createBudget(user, categoryId, currentMonth, BigDecimal.valueOf(1000), null)
                        .getBody();

        assertThat(clientBudget.clientId()).isEqualTo(clientId);
        assertThat(clientBudget.spentValue()).isEqualByComparingTo("120");
        // o orçamento geral da categoria continua somando todas as despesas dela
        assertThat(generalBudget.clientId()).isNull();
        assertThat(generalBudget.spentValue()).isEqualByComparingTo("500");
    }

    @Test
    void rejectsBudgetLinkedToAnotherUsersClient() {
        TestUser owner = TestDataFactory.registerRandomUser(restTemplate);
        TestUser intruder = TestDataFactory.registerRandomUser(restTemplate);
        Long clientId = createClient(owner);
        Long categoryId = createCategory(intruder);

        ResponseEntity<ApiError> response =
                restTemplate.exchange(
                        "/api/budgets",
                        HttpMethod.POST,
                        new HttpEntity<>(
                                new BudgetRequest(
                                        categoryId,
                                        YearMonth.now(),
                                        BigDecimal.valueOf(100),
                                        clientId),
                                intruder.authHeaders()),
                        ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void cannotDeleteClientWithLinkedBudget() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        Long categoryId = createCategory(user);
        Long clientId = createClient(user);
        Long budgetId =
                createBudget(user, categoryId, YearMonth.now(), BigDecimal.valueOf(100), clientId)
                        .getBody()
                        .id();

        ResponseEntity<ApiError> blocked =
                restTemplate.exchange(
                        "/api/clients/" + clientId,
                        HttpMethod.DELETE,
                        new HttpEntity<>(user.authHeaders()),
                        ApiError.class);
        assertThat(blocked.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        restTemplate.exchange(
                "/api/budgets/" + budgetId,
                HttpMethod.DELETE,
                new HttpEntity<>(user.authHeaders()),
                Void.class);
        ResponseEntity<Void> allowed =
                restTemplate.exchange(
                        "/api/clients/" + clientId,
                        HttpMethod.DELETE,
                        new HttpEntity<>(user.authHeaders()),
                        Void.class);
        assertThat(allowed.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    private ResponseEntity<BudgetResponse> createBudget(
            TestUser user, Long categoryId, YearMonth month, BigDecimal limit, Long clientId) {
        return restTemplate.exchange(
                "/api/budgets",
                HttpMethod.POST,
                new HttpEntity<>(
                        new BudgetRequest(categoryId, month, limit, clientId), user.authHeaders()),
                BudgetResponse.class);
    }
}
