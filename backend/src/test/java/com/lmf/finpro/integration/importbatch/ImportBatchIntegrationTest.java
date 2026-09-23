package com.lmf.finpro.integration.importbatch;

import static org.assertj.core.api.Assertions.assertThat;

import com.lmf.finpro.domain.model.AccountType;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.ImportFormat;
import com.lmf.finpro.infrastructure.web.dto.account.AccountRequest;
import com.lmf.finpro.infrastructure.web.dto.account.AccountResponse;
import com.lmf.finpro.infrastructure.web.dto.category.CategoryRequest;
import com.lmf.finpro.infrastructure.web.dto.category.CategoryResponse;
import com.lmf.finpro.infrastructure.web.dto.categoryrule.CategoryRuleResponse;
import com.lmf.finpro.infrastructure.web.dto.importbatch.ImportBatchResponse;
import com.lmf.finpro.infrastructure.web.dto.importbatch.TransactionReviewRequest;
import com.lmf.finpro.infrastructure.web.dto.transaction.TransactionResponse;
import com.lmf.finpro.infrastructure.web.exception.ApiError;
import com.lmf.finpro.integration.support.AbstractIntegrationTest;
import com.lmf.finpro.integration.support.TestDataFactory;
import com.lmf.finpro.integration.support.TestUser;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

class ImportBatchIntegrationTest extends AbstractIntegrationTest {

    private static final String VALID_CSV =
            """
        date,description,amount
        2026-01-05,UBER *TRIP HELP.UBER.COM,-32.50
        2026-01-06,PAGAMENTO CLIENTE ACME,4200.00
        2026-01-07,UBER *TRIP HELP.UBER.COM,-18.90
        """;

    private static final String VALID_OFX =
            """
        <OFX>
        <BANKTRANLIST>
        <STMTTRN>
        <TRNTYPE>DEBIT
        <DTPOSTED>20260105120000
        <TRNAMT>-32.50
        <MEMO>UBER *TRIP HELP.UBER.COM
        </STMTTRN>
        <STMTTRN>
        <TRNTYPE>CREDIT
        <DTPOSTED>20260106120000
        <TRNAMT>4200.00
        <MEMO>PAGAMENTO CLIENTE ACME
        </STMTTRN>
        </BANKTRANLIST>
        </OFX>
        """;

    @Test
    void uploadsCsvAndCategorizesTransactionsUsingExistingRule() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        Long accountId = createAccount(user);
        Long transportCategoryId = createCategory(user, CategoryType.EXPENSE);
        createRule(user, "UBER", transportCategoryId);

        ResponseEntity<ImportBatchResponse> uploadResponse = uploadCsv(user, accountId, VALID_CSV);

        assertThat(uploadResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        ImportBatchResponse batch = uploadResponse.getBody();
        assertThat(batch.transactionCount()).isEqualTo(3);
        // As duas transações do Uber batem com a regra pré-cadastrada; só a do cliente fica sem
        // categoria.
        assertThat(batch.uncategorizedCount()).isEqualTo(1);

        ResponseEntity<TransactionResponse[]> transactionsResponse =
                restTemplate.exchange(
                        "/api/import-batches/" + batch.id() + "/transactions",
                        HttpMethod.GET,
                        new HttpEntity<>(user.authHeaders()),
                        TransactionResponse[].class);
        List<TransactionResponse> transactions = List.of(transactionsResponse.getBody());
        assertThat(transactions).hasSize(3);
        assertThat(transactions).allMatch(t -> "IMPORTED".equals(t.origin().name()));
        assertThat(transactions)
                .filteredOn(t -> t.description().contains("UBER"))
                .allMatch(t -> transportCategoryId.equals(t.categoryId()));
    }

    @Test
    void reviewingImportedTransactionReinforcesCategoryRuleForFutureImports() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        Long accountId = createAccount(user);
        Long incomeCategoryId = createCategory(user, CategoryType.INCOME);

        ImportBatchResponse firstBatch = uploadCsv(user, accountId, VALID_CSV).getBody();
        TransactionResponse[] firstTransactions =
                restTemplate
                        .exchange(
                                "/api/import-batches/" + firstBatch.id() + "/transactions",
                                HttpMethod.GET,
                                new HttpEntity<>(user.authHeaders()),
                                TransactionResponse[].class)
                        .getBody();
        TransactionResponse clientPayment =
                List.of(firstTransactions).stream()
                        .filter(t -> t.description().contains("ACME"))
                        .findFirst()
                        .orElseThrow();
        assertThat(clientPayment.categoryId()).isNull();

        TransactionReviewRequest review = new TransactionReviewRequest(incomeCategoryId, null);
        ResponseEntity<TransactionResponse> reviewResponse =
                restTemplate.exchange(
                        "/api/import-batches/"
                                + firstBatch.id()
                                + "/transactions/"
                                + clientPayment.id(),
                        HttpMethod.PUT,
                        new HttpEntity<>(review, user.authHeaders()),
                        TransactionResponse.class);
        assertThat(reviewResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(reviewResponse.getBody().categoryId()).isEqualTo(incomeCategoryId);

        ResponseEntity<CategoryRuleResponse[]> rulesResponse =
                restTemplate.exchange(
                        "/api/category-rules",
                        HttpMethod.GET,
                        new HttpEntity<>(user.authHeaders()),
                        CategoryRuleResponse[].class);
        assertThat(rulesResponse.getBody())
                .anyMatch(
                        rule ->
                                rule.pattern().contains("ACME")
                                        && incomeCategoryId.equals(rule.categoryId()));

        // Nova importação com o mesmo descritivo já sai categorizada, sem intervenção manual.
        String secondCsv = "date,description,amount\n2026-02-01,PAGAMENTO CLIENTE ACME,3100.00\n";
        ImportBatchResponse secondBatch = uploadCsv(user, accountId, secondCsv).getBody();
        assertThat(secondBatch.uncategorizedCount()).isZero();
    }

    @Test
    void listReturnsAllBatchesForTheOwningUser() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        Long accountId = createAccount(user);
        ImportBatchResponse uploaded = uploadCsv(user, accountId, VALID_CSV).getBody();

        ResponseEntity<ImportBatchResponse[]> response =
                restTemplate.exchange(
                        "/api/import-batches",
                        HttpMethod.GET,
                        new HttpEntity<>(user.authHeaders()),
                        ImportBatchResponse[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(List.of(response.getBody()))
                .extracting(ImportBatchResponse::id)
                .contains(uploaded.id());
    }

    @Test
    void getByIdReturnsOwnBatch() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        Long accountId = createAccount(user);
        ImportBatchResponse uploaded = uploadCsv(user, accountId, VALID_CSV).getBody();

        ResponseEntity<ImportBatchResponse> response =
                restTemplate.exchange(
                        "/api/import-batches/" + uploaded.id(),
                        HttpMethod.GET,
                        new HttpEntity<>(user.authHeaders()),
                        ImportBatchResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().id()).isEqualTo(uploaded.id());
        assertThat(response.getBody().transactionCount()).isEqualTo(3);
    }

    @Test
    void getByIdThrowsWhenBatchBelongsToAnotherUser() {
        TestUser owner = TestDataFactory.registerRandomUser(restTemplate);
        TestUser intruder = TestDataFactory.registerRandomUser(restTemplate);
        Long accountId = createAccount(owner);
        ImportBatchResponse uploaded = uploadCsv(owner, accountId, VALID_CSV).getBody();

        ResponseEntity<ApiError> response =
                restTemplate.exchange(
                        "/api/import-batches/" + uploaded.id(),
                        HttpMethod.GET,
                        new HttpEntity<>(intruder.authHeaders()),
                        ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void uploadsOfxAndCategorizesTransactionsUsingExistingRule() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        Long accountId = createAccount(user);
        Long transportCategoryId = createCategory(user, CategoryType.EXPENSE);
        createRule(user, "UBER", transportCategoryId);

        ResponseEntity<ImportBatchResponse> uploadResponse =
                restTemplate.exchange(
                        "/api/import-batches",
                        HttpMethod.POST,
                        new HttpEntity<>(
                                multipartBody(accountId, VALID_OFX, "extrato.ofx"),
                                multipartHeaders(user)),
                        ImportBatchResponse.class);

        assertThat(uploadResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        ImportBatchResponse batch = uploadResponse.getBody();
        assertThat(batch.format()).isEqualTo(ImportFormat.OFX);
        assertThat(batch.transactionCount()).isEqualTo(2);
        assertThat(batch.uncategorizedCount()).isEqualTo(1);

        ResponseEntity<TransactionResponse[]> transactionsResponse =
                restTemplate.exchange(
                        "/api/import-batches/" + batch.id() + "/transactions",
                        HttpMethod.GET,
                        new HttpEntity<>(user.authHeaders()),
                        TransactionResponse[].class);
        List<TransactionResponse> transactions = List.of(transactionsResponse.getBody());
        assertThat(transactions)
                .filteredOn(t -> t.description().contains("UBER"))
                .allMatch(t -> transportCategoryId.equals(t.categoryId()));
    }

    @Test
    void rejectsOfxWithNoTransactionBlocks() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        Long accountId = createAccount(user);

        ResponseEntity<ApiError> response =
                restTemplate.exchange(
                        "/api/import-batches",
                        HttpMethod.POST,
                        new HttpEntity<>(
                                multipartBody(accountId, "<OFX>\n<BANKTRANLIST>\n</BANKTRANLIST>\n</OFX>\n", "extrato.ofx"),
                                multipartHeaders(user)),
                        ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void rejectsFileWithUnsupportedExtension() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        Long accountId = createAccount(user);

        ResponseEntity<ApiError> response =
                restTemplate.exchange(
                        "/api/import-batches",
                        HttpMethod.POST,
                        new HttpEntity<>(
                                multipartBody(accountId, VALID_CSV, "extrato.txt"),
                                multipartHeaders(user)),
                        ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void rejectsCsvWithInvalidHeader() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        Long accountId = createAccount(user);

        ResponseEntity<ApiError> response =
                restTemplate.exchange(
                        "/api/import-batches",
                        HttpMethod.POST,
                        new HttpEntity<>(
                                multipartBody(accountId, "coluna1,coluna2\nx,y\n"),
                                multipartHeaders(user)),
                        ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void userCannotUploadToAnotherUsersAccount() {
        TestUser owner = TestDataFactory.registerRandomUser(restTemplate);
        TestUser intruder = TestDataFactory.registerRandomUser(restTemplate);
        Long ownerAccountId = createAccount(owner);

        ResponseEntity<ApiError> response =
                restTemplate.exchange(
                        "/api/import-batches",
                        HttpMethod.POST,
                        new HttpEntity<>(
                                multipartBody(ownerAccountId, VALID_CSV),
                                multipartHeaders(intruder)),
                        ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private ResponseEntity<ImportBatchResponse> uploadCsv(
            TestUser user, Long accountId, String csv) {
        return restTemplate.exchange(
                "/api/import-batches",
                HttpMethod.POST,
                new HttpEntity<>(multipartBody(accountId, csv), multipartHeaders(user)),
                ImportBatchResponse.class);
    }

    private MultiValueMap<String, Object> multipartBody(Long accountId, String csv) {
        return multipartBody(accountId, csv, "extrato.csv");
    }

    private MultiValueMap<String, Object> multipartBody(
            Long accountId, String fileContent, String filename) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("accountId", accountId.toString());
        body.add(
                "file",
                new ByteArrayResource(fileContent.getBytes(StandardCharsets.UTF_8)) {
                    @Override
                    public String getFilename() {
                        return filename;
                    }
                });
        return body;
    }

    private HttpHeaders multipartHeaders(TestUser user) {
        HttpHeaders headers = user.authHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        return headers;
    }

    private void createRule(TestUser user, String pattern, Long categoryId) {
        record CategoryRuleRequest(String pattern, Long categoryId) {}
        restTemplate.exchange(
                "/api/category-rules",
                HttpMethod.POST,
                new HttpEntity<>(new CategoryRuleRequest(pattern, categoryId), user.authHeaders()),
                CategoryRuleResponse.class);
    }

    private Long createCategory(TestUser user, CategoryType type) {
        CategoryRequest categoryRequest =
                new CategoryRequest("Categoria " + type, type, null, null);
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
                new AccountRequest("Conta para importação", AccountType.CHECKING, BigDecimal.ZERO);
        ResponseEntity<AccountResponse> response =
                restTemplate.exchange(
                        "/api/accounts",
                        HttpMethod.POST,
                        new HttpEntity<>(accountRequest, user.authHeaders()),
                        AccountResponse.class);
        return response.getBody().id();
    }
}
