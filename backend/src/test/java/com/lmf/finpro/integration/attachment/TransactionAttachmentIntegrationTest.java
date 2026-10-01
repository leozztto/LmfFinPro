package com.lmf.finpro.integration.attachment;

import static org.assertj.core.api.Assertions.assertThat;

import com.lmf.finpro.domain.model.AccountType;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.infrastructure.web.dto.account.AccountRequest;
import com.lmf.finpro.infrastructure.web.dto.account.AccountResponse;
import com.lmf.finpro.infrastructure.web.dto.attachment.TransactionAttachmentResponse;
import com.lmf.finpro.infrastructure.web.dto.transaction.TransactionRequest;
import com.lmf.finpro.infrastructure.web.dto.transaction.TransactionResponse;
import com.lmf.finpro.infrastructure.web.dto.transfer.TransferRequest;
import com.lmf.finpro.infrastructure.web.dto.transfer.TransferResponse;
import com.lmf.finpro.integration.support.AbstractIntegrationTest;
import com.lmf.finpro.integration.support.TestDataFactory;
import com.lmf.finpro.integration.support.TestUser;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

class TransactionAttachmentIntegrationTest extends AbstractIntegrationTest {

    private static final LocalDate DATE = LocalDate.of(2026, 3, 10);

    @Test
    void uploadsListsServesAndCountsAnAttachment() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        Long transactionId =
                createExpense(user, createAccount(user, "Conta"), "Aluguel escritório");
        byte[] png = uniquePng();

        ResponseEntity<TransactionAttachmentResponse> created =
                upload(user, transactionId, png, "comprovante.png", "PAYMENT_PROOF");
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody().contentType()).isEqualTo("image/png");
        assertThat(created.getBody().fileName()).isEqualTo("comprovante.png");
        assertThat(created.getBody().sizeBytes()).isEqualTo(png.length);

        TransactionAttachmentResponse[] listed =
                get(
                                user,
                                "/api/transactions/" + transactionId + "/attachments",
                                TransactionAttachmentResponse[].class)
                        .getBody();
        assertThat(listed).hasSize(1);

        ResponseEntity<byte[]> content =
                get(
                        user,
                        "/api/transactions/"
                                + transactionId
                                + "/attachments/"
                                + created.getBody().id()
                                + "/content",
                        byte[].class);
        assertThat(content.getBody()).isEqualTo(png);
        assertThat(content.getHeaders().getContentType()).isEqualTo(MediaType.IMAGE_PNG);
        assertThat(content.getHeaders().getFirst("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(content.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .startsWith("inline");

        List<TransactionResponse> transactions =
                TestDataFactory.listTransactions(restTemplate, user);
        assertThat(transactions.stream().filter(tx -> tx.id().equals(transactionId)).findFirst())
                .get()
                .extracting(TransactionResponse::attachmentCount)
                .isEqualTo(1L);
    }

    @Test
    void listFiltersByHavingOrNotHavingAttachments() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        Long accountId = createAccount(user, "Conta");
        Long withProof = createExpense(user, accountId, "Com comprovante");
        Long withoutProof = createExpense(user, accountId, "Sem comprovante");
        upload(user, withProof, uniquePng(), "a.png", "PAYMENT_PROOF");

        assertThat(
                        TestDataFactory.transactionsPage(restTemplate, user, "hasAttachment=true")
                                .content())
                .extracting(TransactionResponse::id)
                .containsExactly(withProof);
        assertThat(
                        TestDataFactory.transactionsPage(restTemplate, user, "hasAttachment=false")
                                .content())
                .extracting(TransactionResponse::id)
                .containsExactly(withoutProof);
        assertThat(TestDataFactory.transactionsPage(restTemplate, user, "").totalElements())
                .isEqualTo(2);
    }

    @Test
    void rejectsFilesThatAreNotPdfOrImageEvenWithPdfExtension() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        Long transactionId = createExpense(user, createAccount(user, "Conta"), "Compra");

        ResponseEntity<TransactionAttachmentResponse> response =
                upload(
                        user,
                        transactionId,
                        new byte[] {0x4D, 0x5A, 0x00, 0x01},
                        "nota.pdf",
                        "INVOICE");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void anotherUserCannotListUploadOrDownload() {
        TestUser owner = TestDataFactory.registerRandomUser(restTemplate);
        Long transactionId = createExpense(owner, createAccount(owner, "Conta"), "Compra");
        Long attachmentId =
                upload(owner, transactionId, uniquePng(), "a.png", "OTHER").getBody().id();
        TestUser other = TestDataFactory.registerRandomUser(restTemplate);

        assertThat(
                        get(
                                        other,
                                        "/api/transactions/" + transactionId + "/attachments",
                                        String.class)
                                .getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(
                        get(
                                        other,
                                        "/api/transactions/"
                                                + transactionId
                                                + "/attachments/"
                                                + attachmentId
                                                + "/content",
                                        String.class)
                                .getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(upload(other, transactionId, uniquePng(), "b.png", "OTHER").getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void deletingTheTransactionOrTheTransferRemovesTheFilesFromDisk() throws IOException {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        Long accountId = createAccount(user, "Conta");
        Long transactionId = createExpense(user, accountId, "Compra");
        byte[] transactionFile = uniquePng();
        upload(user, transactionId, transactionFile, "a.png", "OTHER");
        assertThat(storedFileWith(transactionFile)).isTrue();

        restTemplate.exchange(
                "/api/transactions/" + transactionId,
                HttpMethod.DELETE,
                new HttpEntity<>(user.authHeaders()),
                Void.class);
        assertThat(storedFileWith(transactionFile)).isFalse();

        Long otherAccountId = createAccount(user, "Poupança");
        TransferResponse transfer =
                restTemplate
                        .exchange(
                                "/api/transfers",
                                HttpMethod.POST,
                                new HttpEntity<>(
                                        new TransferRequest(
                                                accountId,
                                                otherAccountId,
                                                BigDecimal.TEN,
                                                DATE,
                                                "Reserva"),
                                        user.authHeaders()),
                                TransferResponse.class)
                        .getBody();
        byte[] transferFile = uniquePng();
        upload(user, transfer.fromTransactionId(), transferFile, "b.png", "PAYMENT_PROOF");
        assertThat(storedFileWith(transferFile)).isTrue();

        restTemplate.exchange(
                "/api/transfers/" + transfer.id(),
                HttpMethod.DELETE,
                new HttpEntity<>(user.authHeaders()),
                Void.class);
        assertThat(storedFileWith(transferFile)).isFalse();
    }

    @Test
    void yearArchiveHasTheFilesInMonthFoldersAndAnIndex() throws IOException {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        Long transactionId = createExpense(user, createAccount(user, "Conta"), "Consulta médica");
        byte[] png = uniquePng();
        upload(user, transactionId, png, "recibo.png", "RECEIPT");

        ResponseEntity<byte[]> archive =
                get(user, "/api/reports/attachments-archive?year=2026", byte[].class);

        assertThat(archive.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(archive.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .contains("comprovantes-2026.zip");
        List<String> names = new ArrayList<>();
        String index = null;
        byte[] archivedFile = null;
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(archive.getBody()))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                names.add(entry.getName());
                byte[] bytes = zip.readAllBytes();
                if (entry.getName().endsWith(".csv")) {
                    index = new String(bytes, StandardCharsets.UTF_8);
                } else {
                    archivedFile = bytes;
                }
            }
        }
        assertThat(names).hasSize(2).contains("comprovantes-2026.csv");
        assertThat(names)
                .anyMatch(
                        name ->
                                name.startsWith("2026-03/2026-03-10_consulta-medica_")
                                        && name.endsWith(".png"));
        assertThat(archivedFile).isEqualTo(png);
        assertThat(index).contains("Consulta médica").contains("Recibo").contains("recibo.png");

        assertThat(
                        get(user, "/api/reports/attachments-archive?year=2020", String.class)
                                .getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        // O dispatch ASYNC liberado no SecurityConfig não abre o endpoint: sem token, 401.
        assertThat(
                        restTemplate
                                .exchange(
                                        "/api/reports/attachments-archive?year=2026",
                                        HttpMethod.GET,
                                        HttpEntity.EMPTY,
                                        String.class)
                                .getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    /** PNG com conteúdo único por chamada, para achar o arquivo no disco pelo conteúdo. */
    private static byte[] uniquePng() {
        byte[] signature = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        byte[] marker = UUID.randomUUID().toString().getBytes(StandardCharsets.US_ASCII);
        byte[] content = Arrays.copyOf(signature, signature.length + marker.length);
        System.arraycopy(marker, 0, content, signature.length, marker.length);
        return content;
    }

    private static boolean storedFileWith(byte[] content) throws IOException {
        try (Stream<Path> files = Files.list(ATTACHMENTS_DIR)) {
            return files.anyMatch(
                    file -> {
                        try {
                            return Arrays.equals(Files.readAllBytes(file), content);
                        } catch (IOException e) {
                            return false;
                        }
                    });
        }
    }

    private ResponseEntity<TransactionAttachmentResponse> upload(
            TestUser user,
            Long transactionId,
            byte[] content,
            String fileName,
            String documentType) {
        HttpHeaders headers = user.authHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add(
                "file",
                new ByteArrayResource(content) {
                    @Override
                    public String getFilename() {
                        return fileName;
                    }
                });
        body.add("documentType", documentType);
        return restTemplate.exchange(
                "/api/transactions/" + transactionId + "/attachments",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                TransactionAttachmentResponse.class);
    }

    private <T> ResponseEntity<T> get(TestUser user, String url, Class<T> type) {
        return restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(user.authHeaders()), type);
    }

    private Long createAccount(TestUser user, String name) {
        return restTemplate
                .exchange(
                        "/api/accounts",
                        HttpMethod.POST,
                        new HttpEntity<>(
                                new AccountRequest(
                                        name, AccountType.CHECKING, BigDecimal.valueOf(1000)),
                                user.authHeaders()),
                        AccountResponse.class)
                .getBody()
                .id();
    }

    private Long createExpense(TestUser user, Long accountId, String description) {
        return restTemplate
                .exchange(
                        "/api/transactions",
                        HttpMethod.POST,
                        new HttpEntity<>(
                                new TransactionRequest(
                                        accountId,
                                        null,
                                        null,
                                        description,
                                        BigDecimal.TEN,
                                        DATE,
                                        CategoryType.EXPENSE),
                                user.authHeaders()),
                        TransactionResponse.class)
                .getBody()
                .id();
    }
}
