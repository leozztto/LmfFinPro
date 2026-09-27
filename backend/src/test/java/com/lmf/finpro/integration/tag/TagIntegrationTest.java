package com.lmf.finpro.integration.tag;

import static org.assertj.core.api.Assertions.assertThat;

import com.lmf.finpro.domain.model.AccountType;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.RecurrenceFrequency;
import com.lmf.finpro.domain.model.TransactionStatus;
import com.lmf.finpro.infrastructure.web.dto.account.AccountRequest;
import com.lmf.finpro.infrastructure.web.dto.account.AccountResponse;
import com.lmf.finpro.infrastructure.web.dto.recurringtransaction.RecurringTransactionRequest;
import com.lmf.finpro.infrastructure.web.dto.recurringtransaction.RecurringTransactionResponse;
import com.lmf.finpro.infrastructure.web.dto.tag.TagNamesRequest;
import com.lmf.finpro.infrastructure.web.dto.tag.TagRequest;
import com.lmf.finpro.infrastructure.web.dto.tag.TagResponse;
import com.lmf.finpro.infrastructure.web.dto.tag.TagSummaryResponse;
import com.lmf.finpro.infrastructure.web.dto.transaction.TransactionRequest;
import com.lmf.finpro.infrastructure.web.dto.transaction.TransactionResponse;
import com.lmf.finpro.infrastructure.web.exception.ApiError;
import com.lmf.finpro.integration.support.AbstractIntegrationTest;
import com.lmf.finpro.integration.support.TestDataFactory;
import com.lmf.finpro.integration.support.TestUser;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

class TagIntegrationTest extends AbstractIntegrationTest {

    private static final LocalDate TODAY = LocalDate.now(ZoneId.of("America/Sao_Paulo"));

    private TestUser user;
    private Long accountId;

    @BeforeEach
    void setUp() {
        user = TestDataFactory.registerRandomUser(restTemplate);
        accountId = createAccount(user);
    }

    @Test
    void transactionCreatedWithTagNamesGetsNormalizedTagsCreatedOnTheFly() {
        TransactionResponse created =
                createTransaction(
                        "Hospedagem",
                        "300",
                        CategoryType.EXPENSE,
                        List.of("#Site Acme", "Dedutível", "site-acme"));

        assertThat(created.tags())
                .extracting(TagSummaryResponse::name)
                .containsExactly("dedutível", "site-acme");
        assertThat(listTags(user))
                .extracting(TagResponse::name, TagResponse::transactionCount)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("dedutível", 1L),
                        org.assertj.core.groups.Tuple.tuple("site-acme", 1L));
    }

    @Test
    void tagsOfAPaidTransactionCanBeChangedAndExistingTagsAreReused() {
        createTransaction("A", "10", CategoryType.EXPENSE, List.of("site-acme"));
        TransactionResponse paid =
                createTransaction("B", "20", CategoryType.EXPENSE, List.of("outra"));
        assertThat(paid.status()).isEqualTo(TransactionStatus.PAID);

        ResponseEntity<TransactionResponse> updated =
                restTemplate.exchange(
                        "/api/transactions/" + paid.id() + "/tags",
                        HttpMethod.PUT,
                        new HttpEntity<>(
                                new TagNamesRequest(List.of("SITE ACME")), user.authHeaders()),
                        TransactionResponse.class);

        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updated.getBody().tags())
                .extracting(TagSummaryResponse::name)
                .containsExactly("site-acme");
        assertThat(listTags(user))
                .filteredOn(tag -> tag.name().equals("site-acme"))
                .singleElement()
                .extracting(TagResponse::transactionCount)
                .isEqualTo(2L);
    }

    @Test
    void invalidTagNameRejectsTheWholeTransaction() {
        ResponseEntity<ApiError> response =
                restTemplate.exchange(
                        "/api/transactions",
                        HttpMethod.POST,
                        new HttpEntity<>(
                                transactionRequest("X", "10", List.of("a/b")), user.authHeaders()),
                        ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).contains("caracteres inválidos");
        assertThat(listTransactions(user)).isEmpty();
    }

    @Test
    void renameToAnExistingNameConflictsAndDeletingATagUntagsTransactions() {
        TransactionResponse created =
                createTransaction("A", "10", CategoryType.EXPENSE, List.of("a", "b"));
        Long tagA = tagId(user, "a");

        ResponseEntity<ApiError> conflict =
                restTemplate.exchange(
                        "/api/tags/" + tagA,
                        HttpMethod.PUT,
                        new HttpEntity<>(new TagRequest("#B", null), user.authHeaders()),
                        ApiError.class);
        assertThat(conflict.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        restTemplate.exchange(
                "/api/tags/" + tagA,
                HttpMethod.DELETE,
                new HttpEntity<>(user.authHeaders()),
                Void.class);

        assertThat(listTransactions(user))
                .filteredOn(transaction -> transaction.id().equals(created.id()))
                .singleElement()
                .satisfies(
                        transaction ->
                                assertThat(transaction.tags())
                                        .extracting(TagSummaryResponse::name)
                                        .containsExactly("b"));
    }

    @Test
    void tagsOfAnotherUserAreInvisibleAndUntouchable() {
        createTransaction("A", "10", CategoryType.EXPENSE, List.of("privada"));
        Long tagId = tagId(user, "privada");
        TestUser other = TestDataFactory.registerRandomUser(restTemplate);

        assertThat(listTags(other)).isEmpty();
        ResponseEntity<ApiError> response =
                restTemplate.exchange(
                        "/api/tags/" + tagId,
                        HttpMethod.DELETE,
                        new HttpEntity<>(other.authHeaders()),
                        ApiError.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        ResponseEntity<ApiError> report =
                restTemplate.exchange(
                        "/api/reports/expenses?format=CSV&tagIds=" + tagId,
                        HttpMethod.GET,
                        new HttpEntity<>(other.authHeaders()),
                        ApiError.class);
        assertThat(report.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void recurrenceTagsAreCopiedToEveryGeneratedTransaction() {
        ResponseEntity<RecurringTransactionResponse> recurrence =
                restTemplate.exchange(
                        "/api/recurring-transactions",
                        HttpMethod.POST,
                        new HttpEntity<>(
                                new RecurringTransactionRequest(
                                        accountId,
                                        null,
                                        null,
                                        "Mensalidade Acme",
                                        BigDecimal.valueOf(1000),
                                        CategoryType.INCOME,
                                        RecurrenceFrequency.MONTHLY,
                                        TODAY.minusMonths(1),
                                        null,
                                        List.of("site-acme")),
                                user.authHeaders()),
                        RecurringTransactionResponse.class);

        assertThat(recurrence.getBody().tags())
                .extracting(TagSummaryResponse::name)
                .containsExactly("site-acme");
        List<TransactionResponse> generated = listTransactions(user);
        assertThat(generated).hasSize(2);
        assertThat(generated)
                .allSatisfy(
                        transaction ->
                                assertThat(transaction.tags())
                                        .extracting(TagSummaryResponse::name)
                                        .containsExactly("site-acme"));
    }

    @Test
    void expenseReportFiltersByAnyOfTheTagsWithoutDuplicatesAndShowsTheTagsColumn() {
        createTransaction("Servidor", "100", CategoryType.EXPENSE, List.of("site-acme", "infra"));
        createTransaction("Domínio", "50", CategoryType.EXPENSE, List.of("infra"));
        createTransaction("Mercado", "30", CategoryType.EXPENSE, List.of());
        String query = "tagIds=" + tagId(user, "site-acme") + "&tagIds=" + tagId(user, "infra");

        List<String> lines = csvLines(user, "/api/reports/expenses?format=CSV&" + query);

        assertThat(lines.get(0)).endsWith(";Tags;Moeda original;Valor original");
        List<String> rows = lines.subList(1, lines.size() - 3);
        assertThat(rows).hasSize(2);
        assertThat(rows)
                .anySatisfy(
                        row ->
                                assertThat(row)
                                        .startsWith(TODAY + ";Servidor;")
                                        .endsWith(";#infra #site-acme;;"));
        assertThat(lines.get(lines.size() - 3)).contains(";TOTAL;").contains("150.00");
    }

    @Test
    void tagTotalsReportShowsTheResultOfEachTagAndTheUntaggedRow() {
        createTransaction("Projeto", "5000", CategoryType.INCOME, List.of("site-acme"));
        createTransaction(
                "Servidor", "800", CategoryType.EXPENSE, List.of("site-acme", "dedutível"));
        createTransaction("Mercado", "30", CategoryType.EXPENSE, List.of());

        List<String> lines = csvLines(user, "/api/reports/tag-totals?format=CSV");

        assertThat(lines)
                .containsExactly(
                        "Tag;Lançamentos;Receitas;Despesas;Resultado",
                        "#dedutível;1;0;800.00;-800.00",
                        "#site-acme;2;5000.00;800.00;4200.00",
                        "Sem tag;1;0;30.00;-30.00");

        List<String> filtered =
                csvLines(
                        user,
                        "/api/reports/tag-totals?format=CSV&tagIds=" + tagId(user, "site-acme"));
        assertThat(filtered)
                .containsExactly(
                        "Tag;Lançamentos;Receitas;Despesas;Resultado",
                        "#site-acme;2;5000.00;800.00;4200.00");

        ResponseEntity<byte[]> pdf =
                restTemplate.exchange(
                        "/api/reports/tag-totals",
                        HttpMethod.GET,
                        new HttpEntity<>(user.authHeaders()),
                        byte[].class);
        assertThat(pdf.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(new String(pdf.getBody(), 0, 4, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF");
    }

    private Long createAccount(TestUser owner) {
        return restTemplate
                .exchange(
                        "/api/accounts",
                        HttpMethod.POST,
                        new HttpEntity<>(
                                new AccountRequest(
                                        "Conta", AccountType.CHECKING, BigDecimal.valueOf(1000)),
                                owner.authHeaders()),
                        AccountResponse.class)
                .getBody()
                .id();
    }

    private TransactionRequest transactionRequest(
            String description, String amount, List<String> tagNames) {
        return transactionRequest(description, amount, CategoryType.EXPENSE, tagNames);
    }

    private TransactionRequest transactionRequest(
            String description, String amount, CategoryType type, List<String> tagNames) {
        return new TransactionRequest(
                accountId,
                null,
                null,
                description,
                new BigDecimal(amount),
                TODAY,
                type,
                null,
                tagNames);
    }

    private TransactionResponse createTransaction(
            String description, String amount, CategoryType type, List<String> tagNames) {
        ResponseEntity<TransactionResponse> response =
                restTemplate.exchange(
                        "/api/transactions",
                        HttpMethod.POST,
                        new HttpEntity<>(
                                transactionRequest(description, amount, type, tagNames),
                                user.authHeaders()),
                        TransactionResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }

    private List<TransactionResponse> listTransactions(TestUser owner) {
        return restTemplate
                .exchange(
                        "/api/transactions",
                        HttpMethod.GET,
                        new HttpEntity<>(owner.authHeaders()),
                        new ParameterizedTypeReference<List<TransactionResponse>>() {})
                .getBody();
    }

    private List<TagResponse> listTags(TestUser owner) {
        return restTemplate
                .exchange(
                        "/api/tags",
                        HttpMethod.GET,
                        new HttpEntity<>(owner.authHeaders()),
                        new ParameterizedTypeReference<List<TagResponse>>() {})
                .getBody();
    }

    private Long tagId(TestUser owner, String name) {
        return listTags(owner).stream()
                .filter(tag -> tag.name().equals(name))
                .findFirst()
                .orElseThrow()
                .id();
    }

    private List<String> csvLines(TestUser owner, String url) {
        ResponseEntity<byte[]> response =
                restTemplate.exchange(
                        url, HttpMethod.GET, new HttpEntity<>(owner.authHeaders()), byte[].class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        byte[] body = response.getBody();
        // Pula o BOM UTF-8 (3 bytes) que o CsvWriter coloca para o Excel.
        return Arrays.asList(
                new String(body, 3, body.length - 3, StandardCharsets.UTF_8).split("\r\n"));
    }
}
