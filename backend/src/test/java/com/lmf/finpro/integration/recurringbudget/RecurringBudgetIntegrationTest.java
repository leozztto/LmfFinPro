package com.lmf.finpro.integration.recurringbudget;

import static org.assertj.core.api.Assertions.assertThat;

import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.infrastructure.web.dto.budget.BudgetRequest;
import com.lmf.finpro.infrastructure.web.dto.budget.BudgetResponse;
import com.lmf.finpro.infrastructure.web.dto.category.CategoryRequest;
import com.lmf.finpro.infrastructure.web.dto.category.CategoryResponse;
import com.lmf.finpro.infrastructure.web.dto.recurringbudget.RecurringBudgetRequest;
import com.lmf.finpro.infrastructure.web.dto.recurringbudget.RecurringBudgetResponse;
import com.lmf.finpro.infrastructure.web.dto.recurringbudget.RecurringBudgetUpdateRequest;
import com.lmf.finpro.infrastructure.web.exception.ApiError;
import com.lmf.finpro.integration.support.AbstractIntegrationTest;
import com.lmf.finpro.integration.support.TestDataFactory;
import com.lmf.finpro.integration.support.TestUser;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;

class RecurringBudgetIntegrationTest extends AbstractIntegrationTest {

    private static final YearMonth CURRENT_MONTH = YearMonth.now(ZoneId.of("America/Sao_Paulo"));

    private Long createExpenseCategory(TestUser user) {
        CategoryRequest categoryRequest =
                new CategoryRequest("Mercado", CategoryType.EXPENSE, null, null);
        return restTemplate
                .exchange(
                        "/api/categories",
                        HttpMethod.POST,
                        new HttpEntity<>(categoryRequest, user.authHeaders()),
                        CategoryResponse.class)
                .getBody()
                .id();
    }

    private ResponseEntity<RecurringBudgetResponse> createRecurrence(
            TestUser user, Long categoryId, YearMonth startMonth) {
        RecurringBudgetRequest request =
                new RecurringBudgetRequest(categoryId, BigDecimal.valueOf(500), startMonth, null);
        return restTemplate.exchange(
                "/api/recurring-budgets",
                HttpMethod.POST,
                new HttpEntity<>(request, user.authHeaders()),
                RecurringBudgetResponse.class);
    }

    private List<BudgetResponse> listBudgets(TestUser user) {
        return List.of(
                restTemplate
                        .exchange(
                                "/api/budgets",
                                HttpMethod.GET,
                                new HttpEntity<>(user.authHeaders()),
                                BudgetResponse[].class)
                        .getBody());
    }

    @Test
    void creatingWithPastStartMonthLaunchesMissingBudgets() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        Long categoryId = createExpenseCategory(user);

        ResponseEntity<RecurringBudgetResponse> response =
                createRecurrence(user, categoryId, CURRENT_MONTH.minusMonths(2));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        RecurringBudgetResponse created = response.getBody();
        assertThat(created.generatedMonths()).isEqualTo(3);
        assertThat(created.active()).isTrue();
        assertThat(created.nextGenerationMonth()).isEqualTo(CURRENT_MONTH.plusMonths(1));

        List<BudgetResponse> budgets = listBudgets(user);
        assertThat(budgets).hasSize(3);
        assertThat(budgets)
                .allSatisfy(
                        budget -> {
                            assertThat(budget.categoryId()).isEqualTo(categoryId);
                            assertThat(budget.limitValue()).isEqualByComparingTo("500");
                        });
    }

    @Test
    void creatingWithFutureStartMonthDoesNotLaunchAnythingYet() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        Long categoryId = createExpenseCategory(user);

        RecurringBudgetResponse created =
                createRecurrence(user, categoryId, CURRENT_MONTH.plusMonths(1)).getBody();

        assertThat(created.generatedMonths()).isZero();
        assertThat(created.nextGenerationMonth()).isEqualTo(CURRENT_MONTH.plusMonths(1));
        assertThat(listBudgets(user)).isEmpty();
    }

    @Test
    void doesNotDuplicateAnAlreadyExistingManualBudgetForTheSameMonth() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        Long categoryId = createExpenseCategory(user);
        restTemplate.exchange(
                "/api/budgets",
                HttpMethod.POST,
                new HttpEntity<>(
                        new BudgetRequest(categoryId, CURRENT_MONTH, BigDecimal.valueOf(300)),
                        user.authHeaders()),
                BudgetResponse.class);

        RecurringBudgetResponse created =
                createRecurrence(user, categoryId, CURRENT_MONTH).getBody();

        assertThat(created.generatedMonths()).isEqualTo(1);
        List<BudgetResponse> budgets = listBudgets(user);
        assertThat(budgets).hasSize(1);
        assertThat(budgets.get(0).limitValue()).isEqualByComparingTo("300");
    }

    @Test
    void updatesPausesAndDeletesRecurrenceKeepingLaunchedBudgets() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        Long categoryId = createExpenseCategory(user);
        Long recurrenceId = createRecurrence(user, categoryId, CURRENT_MONTH).getBody().id();

        RecurringBudgetUpdateRequest pause =
                new RecurringBudgetUpdateRequest(BigDecimal.valueOf(650), null, false);
        ResponseEntity<RecurringBudgetResponse> updateResponse =
                restTemplate.exchange(
                        "/api/recurring-budgets/" + recurrenceId,
                        HttpMethod.PUT,
                        new HttpEntity<>(pause, user.authHeaders()),
                        RecurringBudgetResponse.class);
        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updateResponse.getBody().limitValue()).isEqualByComparingTo("650");
        assertThat(updateResponse.getBody().active()).isFalse();
        assertThat(updateResponse.getBody().nextGenerationMonth()).isNull();

        ResponseEntity<Void> deleteResponse =
                restTemplate.exchange(
                        "/api/recurring-budgets/" + recurrenceId,
                        HttpMethod.DELETE,
                        new HttpEntity<>(user.authHeaders()),
                        Void.class);
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        assertThat(listBudgets(user)).hasSize(1);
    }

    @Test
    void rejectsEndMonthBeforeStartMonth() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        Long categoryId = createExpenseCategory(user);
        RecurringBudgetRequest request =
                new RecurringBudgetRequest(
                        categoryId,
                        BigDecimal.valueOf(500),
                        CURRENT_MONTH,
                        CURRENT_MONTH.minusMonths(1));

        ResponseEntity<ApiError> response =
                restTemplate.exchange(
                        "/api/recurring-budgets",
                        HttpMethod.POST,
                        new HttpEntity<>(request, user.authHeaders()),
                        ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void rejectsIncomeCategory() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        CategoryRequest categoryRequest =
                new CategoryRequest("Salário", CategoryType.INCOME, null, null);
        Long categoryId =
                restTemplate
                        .exchange(
                                "/api/categories",
                                HttpMethod.POST,
                                new HttpEntity<>(categoryRequest, user.authHeaders()),
                                CategoryResponse.class)
                        .getBody()
                        .id();

        ResponseEntity<ApiError> response =
                restTemplate.exchange(
                        "/api/recurring-budgets",
                        HttpMethod.POST,
                        new HttpEntity<>(
                                new RecurringBudgetRequest(
                                        categoryId, BigDecimal.valueOf(500), CURRENT_MONTH, null),
                                user.authHeaders()),
                        ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void userCannotAccessAnotherUsersRecurrence() {
        TestUser owner = TestDataFactory.registerRandomUser(restTemplate);
        TestUser intruder = TestDataFactory.registerRandomUser(restTemplate);
        Long categoryId = createExpenseCategory(owner);
        Long recurrenceId =
                createRecurrence(owner, categoryId, CURRENT_MONTH.plusMonths(1)).getBody().id();

        ResponseEntity<ApiError> deleteResponse =
                restTemplate.exchange(
                        "/api/recurring-budgets/" + recurrenceId,
                        HttpMethod.DELETE,
                        new HttpEntity<>(intruder.authHeaders()),
                        ApiError.class);
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
