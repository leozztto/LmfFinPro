package com.lmf.finpro.integration.report;

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
import com.lmf.finpro.integration.support.TestDataFactory;
import com.lmf.finpro.integration.support.TestUser;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;

class ReportIntegrationTest extends AbstractIntegrationTest {

    @Test
    void generatesClientReceiptPdfForOwnedClientAndPeriod() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        Long accountId = createAccount(user);
        Long clientId = createClient(user);
        createIncomeTransaction(
                user,
                accountId,
                clientId,
                "Serviço de consultoria",
                "1000.00",
                LocalDate.of(2026, 9, 5));
        createIncomeTransaction(
                user,
                accountId,
                clientId,
                "Manutenção mensal",
                "500.00",
                LocalDate.of(2026, 9, 20));
        // fora do período pedido — não deve entrar no recibo
        createIncomeTransaction(
                user,
                accountId,
                clientId,
                "Serviço de agosto",
                "300.00",
                LocalDate.of(2026, 8, 15));

        ResponseEntity<byte[]> response =
                restTemplate.exchange(
                        "/api/reports/client-receipt?clientId="
                                + clientId
                                + "&referenceMonth=2026-09",
                        HttpMethod.GET,
                        new HttpEntity<>(user.authHeaders()),
                        byte[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .contains("attachment");
        byte[] body = response.getBody();
        assertThat(body).isNotEmpty();
        assertThat(new String(body, 0, 4, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF");
    }

    @Test
    void returnsNotFoundWhenClientBelongsToAnotherUser() {
        TestUser owner = TestDataFactory.registerRandomUser(restTemplate);
        TestUser intruder = TestDataFactory.registerRandomUser(restTemplate);
        Long clientId = createClient(owner);

        ResponseEntity<ApiError> response =
                restTemplate.exchange(
                        "/api/reports/client-receipt?clientId="
                                + clientId
                                + "&referenceMonth=2026-09",
                        HttpMethod.GET,
                        new HttpEntity<>(intruder.authHeaders()),
                        ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void returnsNotFoundWhenClientDoesNotExist() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);

        ResponseEntity<ApiError> response =
                restTemplate.exchange(
                        "/api/reports/client-receipt?clientId=999999&referenceMonth=2026-09",
                        HttpMethod.GET,
                        new HttpEntity<>(user.authHeaders()),
                        ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void generatesAccountStatementPdfForOwnedAccountAndPeriod() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        Long accountId = createAccount(user);
        createIncomeTransaction(
                user, accountId, null, "Receita do mês", "1000.00", LocalDate.of(2026, 9, 5));
        createExpenseTransaction(
                user, accountId, "Despesa do mês", "300.00", LocalDate.of(2026, 9, 20));
        // fora do período pedido — entra no saldo de abertura, não na lista do extrato
        createIncomeTransaction(
                user, accountId, null, "Receita de agosto", "200.00", LocalDate.of(2026, 8, 15));

        ResponseEntity<byte[]> response =
                restTemplate.exchange(
                        "/api/reports/account-statement?accountId="
                                + accountId
                                + "&referenceMonth=2026-09",
                        HttpMethod.GET,
                        new HttpEntity<>(user.authHeaders()),
                        byte[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .contains("attachment");
        byte[] body = response.getBody();
        assertThat(body).isNotEmpty();
        assertThat(new String(body, 0, 4, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF");
    }

    @Test
    void returnsNotFoundWhenAccountBelongsToAnotherUser() {
        TestUser owner = TestDataFactory.registerRandomUser(restTemplate);
        TestUser intruder = TestDataFactory.registerRandomUser(restTemplate);
        Long accountId = createAccount(owner);

        ResponseEntity<ApiError> response =
                restTemplate.exchange(
                        "/api/reports/account-statement?accountId="
                                + accountId
                                + "&referenceMonth=2026-09",
                        HttpMethod.GET,
                        new HttpEntity<>(intruder.authHeaders()),
                        ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void returnsNotFoundWhenAccountDoesNotExist() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);

        ResponseEntity<ApiError> response =
                restTemplate.exchange(
                        "/api/reports/account-statement?accountId=999999&referenceMonth=2026-09",
                        HttpMethod.GET,
                        new HttpEntity<>(user.authHeaders()),
                        ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void generatesClientAnnualStatementPdfForOwnedClientAndYear() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        Long accountId = createAccount(user);
        Long clientId = createClient(user);
        createIncomeTransaction(
                user,
                accountId,
                clientId,
                "Serviço de janeiro",
                "1000.00",
                LocalDate.of(2026, 1, 10));
        createIncomeTransaction(
                user,
                accountId,
                clientId,
                "Serviço de dezembro",
                "500.00",
                LocalDate.of(2026, 12, 1));
        // fora do ano pedido — não deve entrar no demonstrativo
        createIncomeTransaction(
                user,
                accountId,
                clientId,
                "Serviço do ano anterior",
                "300.00",
                LocalDate.of(2025, 6, 1));

        ResponseEntity<byte[]> response =
                restTemplate.exchange(
                        "/api/reports/client-annual-statement?clientId=" + clientId + "&year=2026",
                        HttpMethod.GET,
                        new HttpEntity<>(user.authHeaders()),
                        byte[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .contains("attachment");
        byte[] body = response.getBody();
        assertThat(body).isNotEmpty();
        assertThat(new String(body, 0, 4, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF");
    }

    @Test
    void returnsNotFoundWhenClientAnnualStatementClientBelongsToAnotherUser() {
        TestUser owner = TestDataFactory.registerRandomUser(restTemplate);
        TestUser intruder = TestDataFactory.registerRandomUser(restTemplate);
        Long clientId = createClient(owner);

        ResponseEntity<ApiError> response =
                restTemplate.exchange(
                        "/api/reports/client-annual-statement?clientId=" + clientId + "&year=2026",
                        HttpMethod.GET,
                        new HttpEntity<>(intruder.authHeaders()),
                        ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void generatesCategoryExpenseReportPdfGroupingExpensesAcrossCategories() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        Long accountId = createAccount(user);
        Long rentCategoryId = createExpenseCategory(user, "Aluguel");
        createExpenseTransaction(
                user,
                accountId,
                rentCategoryId,
                "Aluguel escritório",
                "1500.00",
                LocalDate.of(2026, 9, 5));
        createExpenseTransaction(
                user, accountId, "Despesa sem categoria", "50.00", LocalDate.of(2026, 9, 10));
        createIncomeTransaction(
                user,
                accountId,
                null,
                "Receita não deve entrar",
                "9999.00",
                LocalDate.of(2026, 9, 12));
        // fora do período pedido — não deve entrar no relatório
        createExpenseTransaction(
                user,
                accountId,
                rentCategoryId,
                "Aluguel de agosto",
                "1500.00",
                LocalDate.of(2026, 8, 5));

        ResponseEntity<byte[]> response =
                restTemplate.exchange(
                        "/api/reports/category-expenses?referenceMonth=2026-09",
                        HttpMethod.GET,
                        new HttpEntity<>(user.authHeaders()),
                        byte[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .contains("attachment");
        byte[] body = response.getBody();
        assertThat(body).isNotEmpty();
        assertThat(new String(body, 0, 4, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF");
    }

    @Test
    void generatesCategoryExpenseReportPdfEvenWithNoExpensesInThePeriod() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);

        ResponseEntity<byte[]> response =
                restTemplate.exchange(
                        "/api/reports/category-expenses?referenceMonth=2026-09",
                        HttpMethod.GET,
                        new HttpEntity<>(user.authHeaders()),
                        byte[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        byte[] body = response.getBody();
        assertThat(new String(body, 0, 4, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF");
    }

    @Test
    void generatesIncomeStatementPdfConsolidatingIncomeAndExpensePerMonth() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        Long accountId = createAccount(user);
        createIncomeTransaction(
                user, accountId, null, "Receita de janeiro", "1000.00", LocalDate.of(2026, 1, 10));
        createExpenseTransaction(
                user, accountId, "Despesa de janeiro", "300.00", LocalDate.of(2026, 1, 20));
        // fora do ano pedido — não deve entrar no resultado
        createIncomeTransaction(
                user,
                accountId,
                null,
                "Receita do ano anterior",
                "9999.00",
                LocalDate.of(2025, 12, 31));

        ResponseEntity<byte[]> response =
                restTemplate.exchange(
                        "/api/reports/income-statement?year=2026&granularity=MONTHLY",
                        HttpMethod.GET,
                        new HttpEntity<>(user.authHeaders()),
                        byte[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .contains("attachment");
        byte[] body = response.getBody();
        assertThat(body).isNotEmpty();
        assertThat(new String(body, 0, 4, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF");
    }

    @Test
    void generatesIncomeStatementPdfWithQuarterlyAndYearlyGranularity() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        Long accountId = createAccount(user);
        createIncomeTransaction(
                user, accountId, null, "Receita de março", "2000.00", LocalDate.of(2026, 3, 1));
        createExpenseTransaction(
                user, accountId, "Despesa de novembro", "800.00", LocalDate.of(2026, 11, 1));

        ResponseEntity<byte[]> quarterlyResponse =
                restTemplate.exchange(
                        "/api/reports/income-statement?year=2026&granularity=QUARTERLY",
                        HttpMethod.GET,
                        new HttpEntity<>(user.authHeaders()),
                        byte[].class);
        ResponseEntity<byte[]> yearlyResponse =
                restTemplate.exchange(
                        "/api/reports/income-statement?year=2026&granularity=YEARLY",
                        HttpMethod.GET,
                        new HttpEntity<>(user.authHeaders()),
                        byte[].class);

        assertThat(quarterlyResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(new String(quarterlyResponse.getBody(), 0, 4, StandardCharsets.ISO_8859_1))
                .isEqualTo("%PDF");
        assertThat(yearlyResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(new String(yearlyResponse.getBody(), 0, 4, StandardCharsets.ISO_8859_1))
                .isEqualTo("%PDF");
    }

    @Test
    void generatesIncomeStatementPdfEvenWithNoTransactionsInTheYear() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);

        ResponseEntity<byte[]> response =
                restTemplate.exchange(
                        "/api/reports/income-statement?year=2026&granularity=MONTHLY",
                        HttpMethod.GET,
                        new HttpEntity<>(user.authHeaders()),
                        byte[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        byte[] body = response.getBody();
        assertThat(new String(body, 0, 4, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF");
    }

    @Test
    void generatesBudgetVsActualReportPdfComparingLimitAndSpentPerCategory() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        Long accountId = createAccount(user);
        Long rentCategoryId = createExpenseCategory(user, "Aluguel");
        createBudget(user, rentCategoryId, "2026-09", "1000.00");
        createExpenseTransaction(
                user,
                accountId,
                rentCategoryId,
                "Aluguel escritório",
                "1200.00",
                LocalDate.of(2026, 9, 5));

        ResponseEntity<byte[]> response =
                restTemplate.exchange(
                        "/api/reports/budget-vs-actual?referenceMonth=2026-09",
                        HttpMethod.GET,
                        new HttpEntity<>(user.authHeaders()),
                        byte[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .contains("attachment");
        byte[] body = response.getBody();
        assertThat(body).isNotEmpty();
        assertThat(new String(body, 0, 4, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF");
    }

    @Test
    void generatesBudgetVsActualReportPdfEvenWithNoBudgetsInTheMonth() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);

        ResponseEntity<byte[]> response =
                restTemplate.exchange(
                        "/api/reports/budget-vs-actual?referenceMonth=2026-09",
                        HttpMethod.GET,
                        new HttpEntity<>(user.authHeaders()),
                        byte[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        byte[] body = response.getBody();
        assertThat(new String(body, 0, 4, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF");
    }

    @Test
    void exportsTransactionsToCsvAcrossAllAccountsInThePeriod() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        Long accountId = createAccount(user);
        Long rentCategoryId = createExpenseCategory(user, "Aluguel");
        createExpenseTransaction(
                user,
                accountId,
                rentCategoryId,
                "Aluguel escritório",
                "1500.00",
                LocalDate.of(2026, 9, 5));
        createIncomeTransaction(
                user, accountId, null, "Receita do mês", "1000.00", LocalDate.of(2026, 9, 10));
        // fora do período pedido — não deve entrar na exportação
        createIncomeTransaction(
                user, accountId, null, "Receita de agosto", "200.00", LocalDate.of(2026, 8, 15));

        ResponseEntity<byte[]> response =
                restTemplate.exchange(
                        "/api/reports/transaction-export?referenceMonth=2026-09",
                        HttpMethod.GET,
                        new HttpEntity<>(user.authHeaders()),
                        byte[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .contains("attachment");
        byte[] body = response.getBody();
        assertThat(body).isNotEmpty();
        String content = new String(body, 3, body.length - 3, StandardCharsets.UTF_8);
        assertThat(content).startsWith("Data;Conta;Categoria;Cliente;Descrição;Tipo;Valor\r\n");
        assertThat(content).contains("Aluguel escritório").contains("Receita do mês");
        assertThat(content).doesNotContain("Receita de agosto");
    }

    @Test
    void exportsTransactionsToCsvEvenWithNoTransactionsInThePeriod() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);

        ResponseEntity<byte[]> response =
                restTemplate.exchange(
                        "/api/reports/transaction-export?referenceMonth=2026-09",
                        HttpMethod.GET,
                        new HttpEntity<>(user.authHeaders()),
                        byte[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        byte[] body = response.getBody();
        String content = new String(body, 3, body.length - 3, StandardCharsets.UTF_8);
        assertThat(content).isEqualTo("Data;Conta;Categoria;Cliente;Descrição;Tipo;Valor\r\n");
    }

    @Test
    void generatesClientReceiptCsvWhenFormatIsCsv() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        Long accountId = createAccount(user);
        Long clientId = createClient(user);
        createIncomeTransaction(
                user,
                accountId,
                clientId,
                "Serviço de consultoria",
                "1000.00",
                LocalDate.of(2026, 9, 5));

        ResponseEntity<byte[]> response =
                restTemplate.exchange(
                        "/api/reports/client-receipt?clientId="
                                + clientId
                                + "&referenceMonth=2026-09&format=CSV",
                        HttpMethod.GET,
                        new HttpEntity<>(user.authHeaders()),
                        byte[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType())
                .isEqualTo(MediaType.parseMediaType("text/csv"));
        byte[] body = response.getBody();
        String content = new String(body, 3, body.length - 3, StandardCharsets.UTF_8);
        assertThat(content)
                .startsWith("Data;Descrição;Valor\r\n")
                .contains("Serviço de consultoria");
    }

    @Test
    void generatesAccountStatementCsvWhenFormatIsCsv() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        Long accountId = createAccount(user);
        createIncomeTransaction(
                user, accountId, null, "Receita do mês", "1000.00", LocalDate.of(2026, 9, 5));

        ResponseEntity<byte[]> response =
                restTemplate.exchange(
                        "/api/reports/account-statement?accountId="
                                + accountId
                                + "&referenceMonth=2026-09&format=CSV",
                        HttpMethod.GET,
                        new HttpEntity<>(user.authHeaders()),
                        byte[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType())
                .isEqualTo(MediaType.parseMediaType("text/csv"));
        byte[] body = response.getBody();
        String content = new String(body, 3, body.length - 3, StandardCharsets.UTF_8);
        assertThat(content).startsWith("Data;Descrição;Tipo;Valor;Saldo\r\n");
    }

    @Test
    void generatesClientAnnualStatementCsvWhenFormatIsCsv() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        Long accountId = createAccount(user);
        Long clientId = createClient(user);
        createIncomeTransaction(
                user,
                accountId,
                clientId,
                "Serviço de janeiro",
                "1000.00",
                LocalDate.of(2026, 1, 10));

        ResponseEntity<byte[]> response =
                restTemplate.exchange(
                        "/api/reports/client-annual-statement?clientId="
                                + clientId
                                + "&year=2026&format=CSV",
                        HttpMethod.GET,
                        new HttpEntity<>(user.authHeaders()),
                        byte[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType())
                .isEqualTo(MediaType.parseMediaType("text/csv"));
        byte[] body = response.getBody();
        String content = new String(body, 3, body.length - 3, StandardCharsets.UTF_8);
        assertThat(content).startsWith("Mês;Valor recebido\r\n");
    }

    @Test
    void generatesCategoryExpensesCsvWhenFormatIsCsv() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        Long accountId = createAccount(user);
        Long rentCategoryId = createExpenseCategory(user, "Aluguel");
        createExpenseTransaction(
                user,
                accountId,
                rentCategoryId,
                "Aluguel escritório",
                "1500.00",
                LocalDate.of(2026, 9, 5));

        ResponseEntity<byte[]> response =
                restTemplate.exchange(
                        "/api/reports/category-expenses?referenceMonth=2026-09&format=CSV",
                        HttpMethod.GET,
                        new HttpEntity<>(user.authHeaders()),
                        byte[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType())
                .isEqualTo(MediaType.parseMediaType("text/csv"));
        byte[] body = response.getBody();
        String content = new String(body, 3, body.length - 3, StandardCharsets.UTF_8);
        assertThat(content).startsWith("Categoria;Valor gasto\r\n").contains("Aluguel");
    }

    @Test
    void generatesIncomeStatementCsvWhenFormatIsCsv() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        Long accountId = createAccount(user);
        createIncomeTransaction(
                user, accountId, null, "Receita de janeiro", "1000.00", LocalDate.of(2026, 1, 10));

        ResponseEntity<byte[]> response =
                restTemplate.exchange(
                        "/api/reports/income-statement?year=2026&granularity=YEARLY&format=CSV",
                        HttpMethod.GET,
                        new HttpEntity<>(user.authHeaders()),
                        byte[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType())
                .isEqualTo(MediaType.parseMediaType("text/csv"));
        byte[] body = response.getBody();
        String content = new String(body, 3, body.length - 3, StandardCharsets.UTF_8);
        assertThat(content).startsWith("Período;Receita;Despesa;Resultado\r\n");
    }

    @Test
    void generatesBudgetVsActualCsvWhenFormatIsCsv() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        Long accountId = createAccount(user);
        Long rentCategoryId = createExpenseCategory(user, "Aluguel");
        createBudget(user, rentCategoryId, "2026-09", "1000.00");
        createExpenseTransaction(
                user,
                accountId,
                rentCategoryId,
                "Aluguel escritório",
                "1200.00",
                LocalDate.of(2026, 9, 5));

        ResponseEntity<byte[]> response =
                restTemplate.exchange(
                        "/api/reports/budget-vs-actual?referenceMonth=2026-09&format=CSV",
                        HttpMethod.GET,
                        new HttpEntity<>(user.authHeaders()),
                        byte[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType())
                .isEqualTo(MediaType.parseMediaType("text/csv"));
        byte[] body = response.getBody();
        String content = new String(body, 3, body.length - 3, StandardCharsets.UTF_8);
        assertThat(content).startsWith("Categoria;Limite;Gasto real;Diferença\r\n");
    }

    @Test
    void generatesTransactionExportPdfWhenFormatIsPdf() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        Long accountId = createAccount(user);
        createIncomeTransaction(
                user, accountId, null, "Receita do mês", "1000.00", LocalDate.of(2026, 9, 5));

        ResponseEntity<byte[]> response =
                restTemplate.exchange(
                        "/api/reports/transaction-export?referenceMonth=2026-09&format=PDF",
                        HttpMethod.GET,
                        new HttpEntity<>(user.authHeaders()),
                        byte[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        byte[] body = response.getBody();
        assertThat(new String(body, 0, 4, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF");
    }

    private void createBudget(
            TestUser user, Long categoryId, String referenceMonth, String limitValue) {
        BudgetRequest request =
                new BudgetRequest(
                        categoryId, YearMonth.parse(referenceMonth), new BigDecimal(limitValue));
        restTemplate.exchange(
                "/api/budgets",
                HttpMethod.POST,
                new HttpEntity<>(request, user.authHeaders()),
                BudgetResponse.class);
    }

    private void createExpenseTransaction(
            TestUser user, Long accountId, String description, String amount, LocalDate date) {
        createExpenseTransaction(user, accountId, null, description, amount, date);
    }

    private void createExpenseTransaction(
            TestUser user,
            Long accountId,
            Long categoryId,
            String description,
            String amount,
            LocalDate date) {
        TransactionRequest request =
                new TransactionRequest(
                        accountId,
                        categoryId,
                        null,
                        description,
                        new BigDecimal(amount),
                        date,
                        CategoryType.EXPENSE);
        restTemplate.exchange(
                "/api/transactions",
                HttpMethod.POST,
                new HttpEntity<>(request, user.authHeaders()),
                TransactionResponse.class);
    }

    private Long createExpenseCategory(TestUser user, String name) {
        CategoryRequest request = new CategoryRequest(name, CategoryType.EXPENSE, null, null);
        ResponseEntity<CategoryResponse> response =
                restTemplate.exchange(
                        "/api/categories",
                        HttpMethod.POST,
                        new HttpEntity<>(request, user.authHeaders()),
                        CategoryResponse.class);
        return response.getBody().id();
    }

    private void createIncomeTransaction(
            TestUser user,
            Long accountId,
            Long clientId,
            String description,
            String amount,
            LocalDate date) {
        TransactionRequest request =
                new TransactionRequest(
                        accountId,
                        null,
                        clientId,
                        description,
                        new BigDecimal(amount),
                        date,
                        CategoryType.INCOME);
        restTemplate.exchange(
                "/api/transactions",
                HttpMethod.POST,
                new HttpEntity<>(request, user.authHeaders()),
                TransactionResponse.class);
    }

    private Long createClient(TestUser user) {
        ClientRequest request =
                new ClientRequest(
                        "Cliente do recibo",
                        "cliente.recibo@x.com",
                        "11987654321",
                        DocumentType.CNPJ,
                        "11444777000161",
                        ClientWorkType.PJ,
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

    private Long createAccount(TestUser user) {
        AccountRequest accountRequest =
                new AccountRequest("Conta para recibo", AccountType.CHECKING, BigDecimal.ZERO);
        ResponseEntity<AccountResponse> response =
                restTemplate.exchange(
                        "/api/accounts",
                        HttpMethod.POST,
                        new HttpEntity<>(accountRequest, user.authHeaders()),
                        AccountResponse.class);
        return response.getBody().id();
    }
}
