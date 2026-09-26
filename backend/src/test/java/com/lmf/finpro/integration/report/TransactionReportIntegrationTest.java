package com.lmf.finpro.integration.report;

import static org.assertj.core.api.Assertions.assertThat;

import com.lmf.finpro.domain.model.AccountScope;
import com.lmf.finpro.domain.model.AccountType;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.TransactionStatus;
import com.lmf.finpro.infrastructure.web.dto.account.AccountRequest;
import com.lmf.finpro.infrastructure.web.dto.account.AccountResponse;
import com.lmf.finpro.infrastructure.web.dto.category.CategoryRequest;
import com.lmf.finpro.infrastructure.web.dto.category.CategoryResponse;
import com.lmf.finpro.infrastructure.web.dto.transaction.TransactionRequest;
import com.lmf.finpro.infrastructure.web.dto.transaction.TransactionResponse;
import com.lmf.finpro.infrastructure.web.dto.transfer.TransferRequest;
import com.lmf.finpro.infrastructure.web.dto.transfer.TransferResponse;
import com.lmf.finpro.integration.support.AbstractIntegrationTest;
import com.lmf.finpro.integration.support.TestDataFactory;
import com.lmf.finpro.integration.support.TestUser;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;

class TransactionReportIntegrationTest extends AbstractIntegrationTest {

    private TestUser user;
    private Long businessAccountId;
    private Long rentCategoryId;

    /**
     * Despesas: aluguel PJ pago (1500, 05/08), internet PJ pendente (120, 20/08) e farmácia PF paga
     * sem categoria (80, 10/07). Receita: projeto PJ (5000, 10/08). Uma transferência PJ → PF, que
     * não pode aparecer em nenhum dos dois relatórios.
     */
    @BeforeEach
    void setUp() {
        user = TestDataFactory.registerRandomUser(restTemplate);
        businessAccountId = createAccount("Conta PJ", AccountScope.BUSINESS);
        Long personalAccountId = createAccount("Conta PF", AccountScope.PERSONAL);
        rentCategoryId = createCategory("Aluguel", CategoryType.EXPENSE);
        Long internetCategoryId = createCategory("Internet", CategoryType.EXPENSE);
        Long servicesCategoryId = createCategory("Serviços", CategoryType.INCOME);

        createTransaction(
                businessAccountId,
                rentCategoryId,
                "Aluguel escritório",
                "1500",
                LocalDate.of(2026, 8, 5),
                CategoryType.EXPENSE,
                TransactionStatus.PAID);
        createTransaction(
                businessAccountId,
                internetCategoryId,
                "Internet fibra",
                "120",
                LocalDate.of(2026, 8, 20),
                CategoryType.EXPENSE,
                TransactionStatus.PENDING);
        createTransaction(
                personalAccountId,
                null,
                "Farmácia",
                "80",
                LocalDate.of(2026, 7, 10),
                CategoryType.EXPENSE,
                TransactionStatus.PAID);
        createTransaction(
                businessAccountId,
                servicesCategoryId,
                "Projeto X",
                "5000",
                LocalDate.of(2026, 8, 10),
                CategoryType.INCOME,
                TransactionStatus.PAID);

        ResponseEntity<TransferResponse> transfer =
                restTemplate.exchange(
                        "/api/transfers",
                        HttpMethod.POST,
                        new HttpEntity<>(
                                new TransferRequest(
                                        businessAccountId,
                                        personalAccountId,
                                        BigDecimal.valueOf(300),
                                        LocalDate.of(2026, 8, 15),
                                        "Pró-labore"),
                                user.authHeaders()),
                        TransferResponse.class);
        assertThat(transfer.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void expensesWithoutFiltersListEveryExpenseButNotTransfers() {
        List<String> rows = csvRows("/api/reports/expenses?format=CSV");

        assertThat(rows).hasSize(3);
        assertThat(total("/api/reports/expenses?format=CSV")).isEqualTo("1700.00");
    }

    @Test
    void periodAndAccountScopeNarrowTheExpenses() {
        String url =
                "/api/reports/expenses?startDate=2026-08-01&endDate=2026-08-31"
                        + "&accountScope=BUSINESS&format=CSV";

        assertThat(csvRows(url)).hasSize(2);
        assertThat(total(url)).isEqualTo("1620.00");
    }

    @Test
    void statusDescriptionAmountAndCategoryFilters() {
        assertThat(csvRows("/api/reports/expenses?status=PENDING&format=CSV"))
                .singleElement()
                .asString()
                .contains("Internet fibra");
        assertThat(csvRows("/api/reports/expenses?description=ALUGUEL&minAmount=1000&format=CSV"))
                .singleElement()
                .asString()
                .contains("Aluguel escritório");
        assertThat(csvRows("/api/reports/expenses?maxAmount=100&format=CSV"))
                .singleElement()
                .asString()
                .contains("Farmácia");
        assertThat(
                        csvRows(
                                "/api/reports/expenses?categoryId="
                                        + rentCategoryId
                                        + "&accountId="
                                        + businessAccountId
                                        + "&format=CSV"))
                .hasSize(1);
    }

    @Test
    void emptyParametersAreIgnored() {
        assertThat(csvRows("/api/reports/expenses?startDate=&categoryId=&description=&format=CSV"))
                .hasSize(3);
    }

    @Test
    void incomesExcludeTheTransferCreditAndDefaultToPdf() {
        assertThat(csvRows("/api/reports/incomes?format=CSV"))
                .singleElement()
                .asString()
                .contains("Projeto X");

        ResponseEntity<byte[]> pdf = get("/api/reports/incomes?startDate=2026-08-01");
        assertThat(pdf.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(pdf.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(new String(pdf.getBody(), 0, 4, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF");
    }

    @Test
    void invalidFiltersAreRejected() {
        assertThat(
                        get("/api/reports/expenses?startDate=2026-09-01&endDate=2026-08-01")
                                .getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(get("/api/reports/expenses?minAmount=500&maxAmount=100").getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        TestUser other = TestDataFactory.registerRandomUser(restTemplate);
        ResponseEntity<byte[]> foreignAccount =
                restTemplate.exchange(
                        "/api/reports/expenses?accountId=" + businessAccountId,
                        HttpMethod.GET,
                        new HttpEntity<>(other.authHeaders()),
                        byte[].class);
        assertThat(foreignAccount.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    /** Linhas de dados do CSV: sem o cabeçalho e sem as três linhas de total. */
    private List<String> csvRows(String url) {
        List<String> lines = csvLines(url);
        return lines.subList(1, lines.size() - 3);
    }

    private String total(String url) {
        List<String> lines = csvLines(url);
        String[] totalLine = lines.get(lines.size() - 3).split(";");
        return totalLine[totalLine.length - 1];
    }

    private List<String> csvLines(String url) {
        ResponseEntity<byte[]> response = get(url);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        byte[] body = response.getBody();
        // Pula o BOM UTF-8 (3 bytes) que o CsvWriter coloca para o Excel.
        String content = new String(body, 3, body.length - 3, StandardCharsets.UTF_8);
        return Arrays.asList(content.split("\r\n"));
    }

    private ResponseEntity<byte[]> get(String url) {
        return restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(user.authHeaders()), byte[].class);
    }

    private Long createAccount(String name, AccountScope scope) {
        return restTemplate
                .exchange(
                        "/api/accounts",
                        HttpMethod.POST,
                        new HttpEntity<>(
                                new AccountRequest(
                                        name,
                                        AccountType.CHECKING,
                                        BigDecimal.valueOf(10000),
                                        scope),
                                user.authHeaders()),
                        AccountResponse.class)
                .getBody()
                .id();
    }

    private Long createCategory(String name, CategoryType type) {
        return restTemplate
                .exchange(
                        "/api/categories",
                        HttpMethod.POST,
                        new HttpEntity<>(
                                new CategoryRequest(name, type, null, null), user.authHeaders()),
                        CategoryResponse.class)
                .getBody()
                .id();
    }

    private void createTransaction(
            Long accountId,
            Long categoryId,
            String description,
            String amount,
            LocalDate date,
            CategoryType type,
            TransactionStatus status) {
        ResponseEntity<TransactionResponse> response =
                restTemplate.exchange(
                        "/api/transactions",
                        HttpMethod.POST,
                        new HttpEntity<>(
                                new TransactionRequest(
                                        accountId,
                                        categoryId,
                                        null,
                                        description,
                                        new BigDecimal(amount),
                                        date,
                                        type,
                                        status),
                                user.authHeaders()),
                        TransactionResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }
}
