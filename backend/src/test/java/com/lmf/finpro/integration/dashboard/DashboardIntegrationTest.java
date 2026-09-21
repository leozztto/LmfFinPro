package com.lmf.finpro.integration.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

import com.lmf.finpro.domain.model.AccountType;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.infrastructure.web.dto.account.AccountRequest;
import com.lmf.finpro.infrastructure.web.dto.account.AccountResponse;
import com.lmf.finpro.infrastructure.web.dto.category.CategoryRequest;
import com.lmf.finpro.infrastructure.web.dto.category.CategoryResponse;
import com.lmf.finpro.infrastructure.web.dto.client.ClientRequest;
import com.lmf.finpro.infrastructure.web.dto.client.ClientResponse;
import com.lmf.finpro.infrastructure.web.dto.dashboard.BalancePointResponse;
import com.lmf.finpro.infrastructure.web.dto.dashboard.BreakdownPointResponse;
import com.lmf.finpro.infrastructure.web.dto.dashboard.CashFlowProjectionPointResponse;
import com.lmf.finpro.infrastructure.web.dto.dashboard.DashboardOverviewResponse;
import com.lmf.finpro.infrastructure.web.dto.dashboard.MonthlyFlowPointResponse;
import com.lmf.finpro.infrastructure.web.dto.transaction.TransactionRequest;
import com.lmf.finpro.infrastructure.web.dto.transaction.TransactionResponse;
import com.lmf.finpro.infrastructure.web.exception.ApiError;
import com.lmf.finpro.integration.support.AbstractIntegrationTest;
import com.lmf.finpro.integration.support.TestDataFactory;
import com.lmf.finpro.integration.support.TestUser;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;

class DashboardIntegrationTest extends AbstractIntegrationTest {

    @Test
    void categoryBreakdownRejectsInvalidMonthFormat() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);

        ResponseEntity<ApiError> response =
                restTemplate.exchange(
                        "/api/dashboard/category-breakdown?type=EXPENSE&month=not-a-month",
                        HttpMethod.GET,
                        new HttpEntity<>(user.authHeaders()),
                        ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).isEqualTo("mês inválido, use o formato aaaa-MM");
    }

    @Test
    void overviewReturnsCurrentBalanceAndMonthOverMonthDeltas() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        Long accountId = createAccount(user, BigDecimal.valueOf(1000));
        YearMonth currentMonth = YearMonth.now();
        YearMonth previousMonth = currentMonth.minusMonths(1);

        createTransaction(
                user,
                accountId,
                null,
                null,
                BigDecimal.valueOf(300),
                previousMonth.atDay(1),
                CategoryType.INCOME);
        createTransaction(
                user,
                accountId,
                null,
                null,
                BigDecimal.valueOf(100),
                previousMonth.atDay(2),
                CategoryType.EXPENSE);
        createTransaction(
                user,
                accountId,
                null,
                null,
                BigDecimal.valueOf(500),
                currentMonth.atDay(1),
                CategoryType.INCOME);
        createTransaction(
                user,
                accountId,
                null,
                null,
                BigDecimal.valueOf(200),
                currentMonth.atDay(2),
                CategoryType.EXPENSE);

        DashboardOverviewResponse overview = getOverview(user);

        // saldo = 1000 (inicial) + 300 + 500 (receitas) - 100 - 200 (despesas)
        assertThat(overview.currentBalance()).isEqualByComparingTo("1500");
        assertThat(overview.currentMonthIncome()).isEqualByComparingTo("500");
        assertThat(overview.currentMonthExpense()).isEqualByComparingTo("200");
        // saldo do mês anterior = 1000 + 300 - 100 = 1200; delta = (1500-1200)/1200*100 = 25%
        assertThat(overview.balanceDeltaPercent()).isCloseTo(25.0, offset(0.01));
        // receita: (500-300)/300*100 ≈ 66.67%
        assertThat(overview.incomeDeltaPercent()).isCloseTo(66.67, offset(0.01));
        // despesa: (200-100)/100*100 = 100%
        assertThat(overview.expenseDeltaPercent()).isCloseTo(100.0, offset(0.01));
    }

    @Test
    void monthlyFlowReturnsRequestedNumberOfMonthsEndingInCurrentMonth() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        Long accountId = createAccount(user, BigDecimal.ZERO);
        YearMonth currentMonth = YearMonth.now();
        createTransaction(
                user,
                accountId,
                null,
                null,
                BigDecimal.valueOf(700),
                currentMonth.atDay(1),
                CategoryType.INCOME);

        ResponseEntity<MonthlyFlowPointResponse[]> response =
                restTemplate.exchange(
                        "/api/dashboard/monthly-flow?months=3",
                        HttpMethod.GET,
                        new HttpEntity<>(user.authHeaders()),
                        MonthlyFlowPointResponse[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<MonthlyFlowPointResponse> points = List.of(response.getBody());
        assertThat(points).hasSize(3);
        assertThat(points.get(2).month()).isEqualTo(currentMonth);
        assertThat(points.get(2).income()).isEqualByComparingTo("700");
    }

    @Test
    void balanceEvolutionLastPointMatchesOverviewCurrentBalanceWhenNoFutureTransactions() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        Long accountId = createAccount(user, BigDecimal.valueOf(1000));
        createTransaction(
                user,
                accountId,
                null,
                null,
                BigDecimal.valueOf(500),
                YearMonth.now().atDay(1),
                CategoryType.INCOME);

        DashboardOverviewResponse overview = getOverview(user);

        ResponseEntity<BalancePointResponse[]> response =
                restTemplate.exchange(
                        "/api/dashboard/balance-evolution?months=6",
                        HttpMethod.GET,
                        new HttpEntity<>(user.authHeaders()),
                        BalancePointResponse[].class);
        List<BalancePointResponse> points = List.of(response.getBody());

        assertThat(points).hasSize(6);
        assertThat(points.get(5).month()).isEqualTo(YearMonth.now());
        assertThat(points.get(5).balance()).isEqualByComparingTo(overview.currentBalance());
    }

    @Test
    void cashFlowProjectionUsesRealFutureTransactionWhenAlreadyScheduled() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        Long accountId = createAccount(user, BigDecimal.valueOf(1000));
        YearMonth nextMonth = YearMonth.now().plusMonths(1);
        createTransaction(
                user,
                accountId,
                null,
                null,
                BigDecimal.valueOf(900),
                nextMonth.atDay(5),
                CategoryType.INCOME);

        ResponseEntity<CashFlowProjectionPointResponse[]> response =
                restTemplate.exchange(
                        "/api/dashboard/cash-flow-projection?months=3",
                        HttpMethod.GET,
                        new HttpEntity<>(user.authHeaders()),
                        CashFlowProjectionPointResponse[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<CashFlowProjectionPointResponse> points = List.of(response.getBody());
        assertThat(points).hasSize(3);
        assertThat(points.get(0).month()).isEqualTo(nextMonth);
        assertThat(points.get(0).projected()).isTrue();
        // saldo atual (1000) + receita real de 900 já lançada no próximo mês = 1900
        assertThat(points.get(0).balance()).isEqualByComparingTo("1900");
    }

    @Test
    void categoryBreakdownGroupsExpensesByCategoryForGivenMonth() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        Long accountId = createAccount(user, BigDecimal.ZERO);
        Long foodCategoryId = createCategory(user, "Alimentação", CategoryType.EXPENSE);
        Long transportCategoryId = createCategory(user, "Transporte", CategoryType.EXPENSE);
        YearMonth currentMonth = YearMonth.now();

        createTransaction(
                user,
                accountId,
                foodCategoryId,
                null,
                BigDecimal.valueOf(150),
                currentMonth.atDay(1),
                CategoryType.EXPENSE);
        createTransaction(
                user,
                accountId,
                foodCategoryId,
                null,
                BigDecimal.valueOf(50),
                currentMonth.atDay(2),
                CategoryType.EXPENSE);
        createTransaction(
                user,
                accountId,
                transportCategoryId,
                null,
                BigDecimal.valueOf(80),
                currentMonth.atDay(3),
                CategoryType.EXPENSE);

        ResponseEntity<BreakdownPointResponse[]> response =
                restTemplate.exchange(
                        "/api/dashboard/category-breakdown?type=EXPENSE&month=" + currentMonth,
                        HttpMethod.GET,
                        new HttpEntity<>(user.authHeaders()),
                        BreakdownPointResponse[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<BreakdownPointResponse> points = List.of(response.getBody());
        assertThat(points).hasSize(2);
        assertThat(points.get(0).entityId()).isEqualTo(foodCategoryId);
        assertThat(points.get(0).value()).isEqualByComparingTo("200");
        assertThat(points.get(1).entityId()).isEqualTo(transportCategoryId);
        assertThat(points.get(1).value()).isEqualByComparingTo("80");
    }

    @Test
    void clientBreakdownGroupsIncomeByClientForGivenMonth() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        Long accountId = createAccount(user, BigDecimal.ZERO);
        Long clientId = createClient(user, "Cliente A");
        YearMonth currentMonth = YearMonth.now();

        createTransaction(
                user,
                accountId,
                null,
                clientId,
                BigDecimal.valueOf(1200),
                currentMonth.atDay(1),
                CategoryType.INCOME);
        createTransaction(
                user,
                accountId,
                null,
                null,
                BigDecimal.valueOf(300),
                currentMonth.atDay(2),
                CategoryType.INCOME);

        ResponseEntity<BreakdownPointResponse[]> response =
                restTemplate.exchange(
                        "/api/dashboard/client-breakdown?month=" + currentMonth,
                        HttpMethod.GET,
                        new HttpEntity<>(user.authHeaders()),
                        BreakdownPointResponse[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<BreakdownPointResponse> points = List.of(response.getBody());
        assertThat(points).hasSize(2);
        assertThat(points)
                .anySatisfy(
                        point -> {
                            assertThat(point.entityId()).isEqualTo(clientId);
                            assertThat(point.value()).isEqualByComparingTo("1200");
                        });
        assertThat(points)
                .anySatisfy(
                        point -> {
                            assertThat(point.entityId()).isNull();
                            assertThat(point.value()).isEqualByComparingTo("300");
                        });
    }

    private DashboardOverviewResponse getOverview(TestUser user) {
        ResponseEntity<DashboardOverviewResponse> response =
                restTemplate.exchange(
                        "/api/dashboard/overview",
                        HttpMethod.GET,
                        new HttpEntity<>(user.authHeaders()),
                        DashboardOverviewResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    private Long createAccount(TestUser user, BigDecimal initialBalance) {
        AccountRequest request =
                new AccountRequest("Conta Dashboard", AccountType.CHECKING, initialBalance);
        ResponseEntity<AccountResponse> response =
                restTemplate.exchange(
                        "/api/accounts",
                        HttpMethod.POST,
                        new HttpEntity<>(request, user.authHeaders()),
                        AccountResponse.class);
        return response.getBody().id();
    }

    private Long createCategory(TestUser user, String name, CategoryType type) {
        CategoryRequest request = new CategoryRequest(name, type, null, null);
        ResponseEntity<CategoryResponse> response =
                restTemplate.exchange(
                        "/api/categories",
                        HttpMethod.POST,
                        new HttpEntity<>(request, user.authHeaders()),
                        CategoryResponse.class);
        return response.getBody().id();
    }

    private Long createClient(TestUser user, String name) {
        ClientRequest request =
                new ClientRequest(
                        name,
                        "cliente@teste.com",
                        "11987654321",
                        com.lmf.finpro.domain.model.DocumentType.CPF,
                        "52998224725",
                        com.lmf.finpro.domain.model.ClientWorkType.PJ,
                        null,
                        null,
                        true);
        ResponseEntity<ClientResponse> response =
                restTemplate.exchange(
                        "/api/clients",
                        HttpMethod.POST,
                        new HttpEntity<>(request, user.authHeaders()),
                        ClientResponse.class);
        return response.getBody().id();
    }

    private void createTransaction(
            TestUser user,
            Long accountId,
            Long categoryId,
            Long clientId,
            BigDecimal amount,
            LocalDate date,
            CategoryType type) {
        TransactionRequest request =
                new TransactionRequest(
                        accountId, categoryId, clientId, "Movimento", amount, date, type);
        ResponseEntity<TransactionResponse> response =
                restTemplate.exchange(
                        "/api/transactions",
                        HttpMethod.POST,
                        new HttpEntity<>(request, user.authHeaders()),
                        TransactionResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }
}
