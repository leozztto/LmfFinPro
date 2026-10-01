package com.lmf.finpro.integration.transaction;

import static org.assertj.core.api.Assertions.assertThat;

import com.lmf.finpro.domain.model.AccountType;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.TransactionStatus;
import com.lmf.finpro.infrastructure.web.dto.account.AccountRequest;
import com.lmf.finpro.infrastructure.web.dto.account.AccountResponse;
import com.lmf.finpro.infrastructure.web.dto.common.PageResponse;
import com.lmf.finpro.infrastructure.web.dto.transaction.TransactionRequest;
import com.lmf.finpro.infrastructure.web.dto.transaction.TransactionResponse;
import com.lmf.finpro.integration.support.AbstractIntegrationTest;
import com.lmf.finpro.integration.support.TestDataFactory;
import com.lmf.finpro.integration.support.TestUser;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;

/** Paginação e filtros de {@code GET /api/transactions}, resolvidos no banco. */
class TransactionListIntegrationTest extends AbstractIntegrationTest {

    private static final LocalDate BASE = LocalDate.of(2026, 1, 1);

    @Test
    void pagesThroughTheStatementFromNewestToOldest() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        Long accountId = createAccount(user);
        for (int day = 0; day < 25; day++) {
            create(user, accountId, "Lançamento " + day, BASE.plusDays(day), CategoryType.EXPENSE);
        }

        PageResponse<TransactionResponse> first =
                TestDataFactory.transactionsPage(restTemplate, user, "page=0&size=10");
        PageResponse<TransactionResponse> last =
                TestDataFactory.transactionsPage(restTemplate, user, "page=2&size=10");

        assertThat(first.totalElements()).isEqualTo(25);
        assertThat(first.totalPages()).isEqualTo(3);
        assertThat(first.content()).hasSize(10);
        assertThat(first.content().get(0).description()).isEqualTo("Lançamento 24");
        assertThat(first.content().get(9).description()).isEqualTo("Lançamento 15");
        assertThat(last.content()).hasSize(5);
        assertThat(last.content().get(4).description()).isEqualTo("Lançamento 0");
    }

    @Test
    void defaultsTo20PerPageAndCapsTheSizeAt100() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        Long accountId = createAccount(user);
        for (int day = 0; day < 25; day++) {
            create(user, accountId, "Lançamento " + day, BASE.plusDays(day), CategoryType.EXPENSE);
        }

        assertThat(TestDataFactory.transactionsPage(restTemplate, user, "").content()).hasSize(20);
        PageResponse<TransactionResponse> capped =
                TestDataFactory.transactionsPage(restTemplate, user, "size=5000");
        assertThat(capped.size()).isEqualTo(100);
        assertThat(capped.content()).hasSize(25);
    }

    @Test
    void filtersAreAppliedBeforePaginating() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        Long accountId = createAccount(user);
        Long otherAccountId = createAccount(user);
        for (int day = 0; day < 6; day++) {
            create(user, accountId, "Despesa " + day, BASE.plusDays(day), CategoryType.EXPENSE);
        }
        create(user, accountId, "Receita A", BASE.plusDays(2), CategoryType.INCOME);
        create(user, otherAccountId, "Receita B", BASE.plusDays(10), CategoryType.INCOME);
        create(
                user,
                accountId,
                "Receita pendente",
                BASE.plusDays(3),
                CategoryType.INCOME,
                TransactionStatus.PENDING,
                List.of("projeto-acme"));

        PageResponse<TransactionResponse> incomes =
                TestDataFactory.transactionsPage(restTemplate, user, "type=INCOME&size=2");
        assertThat(incomes.totalElements()).isEqualTo(3);
        assertThat(incomes.totalPages()).isEqualTo(2);

        assertThat(
                        TestDataFactory.transactionsPage(
                                        restTemplate, user, "accountId=" + otherAccountId)
                                .content())
                .extracting(TransactionResponse::description)
                .containsExactly("Receita B");

        assertThat(
                        TestDataFactory.transactionsPage(
                                        restTemplate,
                                        user,
                                        "startDate=2026-01-03&endDate=2026-01-04")
                                .content())
                .extracting(TransactionResponse::description)
                .containsExactlyInAnyOrder(
                        "Despesa 2", "Despesa 3", "Receita A", "Receita pendente");

        assertThat(TestDataFactory.transactionsPage(restTemplate, user, "status=PENDING").content())
                .extracting(TransactionResponse::description)
                .containsExactly("Receita pendente");

        assertThat(
                        TestDataFactory.transactionsPage(
                                        restTemplate,
                                        user,
                                        "tagNames=Projeto-Acme&tagNames=nao-existe")
                                .content())
                .extracting(TransactionResponse::description)
                .containsExactly("Receita pendente");
    }

    @Test
    void unknownTagYieldsAnEmptyPage() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        create(user, createAccount(user), "Qualquer", BASE, CategoryType.EXPENSE);

        PageResponse<TransactionResponse> page =
                TestDataFactory.transactionsPage(restTemplate, user, "tagNames=fantasma");

        assertThat(page.content()).isEmpty();
        assertThat(page.totalElements()).isZero();
    }

    @Test
    void userOnlySeesOwnTransactions() {
        TestUser owner = TestDataFactory.registerRandomUser(restTemplate);
        TestUser stranger = TestDataFactory.registerRandomUser(restTemplate);
        Long accountId = createAccount(owner);
        create(owner, accountId, "Só do dono", BASE, CategoryType.EXPENSE);

        assertThat(TestDataFactory.transactionsPage(restTemplate, stranger, "").content())
                .isEmpty();
        assertThat(
                        TestDataFactory.transactionsPage(
                                        restTemplate, stranger, "accountId=" + accountId)
                                .totalElements())
                .isZero();
    }

    private Long createAccount(TestUser user) {
        return restTemplate
                .exchange(
                        "/api/accounts",
                        HttpMethod.POST,
                        new HttpEntity<>(
                                new AccountRequest("Conta", AccountType.CHECKING, BigDecimal.ZERO),
                                user.authHeaders()),
                        AccountResponse.class)
                .getBody()
                .id();
    }

    private void create(
            TestUser user, Long accountId, String description, LocalDate date, CategoryType type) {
        create(user, accountId, description, date, type, TransactionStatus.PAID, List.of());
    }

    private void create(
            TestUser user,
            Long accountId,
            String description,
            LocalDate date,
            CategoryType type,
            TransactionStatus status,
            List<String> tagNames) {
        restTemplate.exchange(
                "/api/transactions",
                HttpMethod.POST,
                new HttpEntity<>(
                        new TransactionRequest(
                                accountId,
                                null,
                                null,
                                description,
                                BigDecimal.TEN,
                                date,
                                type,
                                status,
                                tagNames),
                        user.authHeaders()),
                TransactionResponse.class);
    }
}
