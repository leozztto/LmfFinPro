package com.lmf.finpro.application.attachment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lmf.finpro.domain.exception.AttachmentInvalidException;
import com.lmf.finpro.domain.exception.ResourceNotFoundException;
import com.lmf.finpro.domain.model.Account;
import com.lmf.finpro.domain.model.AccountType;
import com.lmf.finpro.domain.model.AttachmentDocumentType;
import com.lmf.finpro.domain.model.AttachmentFileType;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.Transaction;
import com.lmf.finpro.domain.model.TransactionAttachment;
import com.lmf.finpro.domain.model.TransactionOrigin;
import com.lmf.finpro.domain.model.TransactionStatus;
import com.lmf.finpro.domain.port.out.AccountRepositoryPort;
import com.lmf.finpro.domain.port.out.FileStoragePort;
import com.lmf.finpro.domain.port.out.TransactionAttachmentRepositoryPort;
import com.lmf.finpro.domain.port.out.TransactionRepositoryPort;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransactionAttachmentApplicationServiceTest {

    private static final Long USER_ID = 10L;
    private static final Long TRANSACTION_ID = 7L;
    private static final byte[] PDF = "%PDF-1.7 conteudo".getBytes(StandardCharsets.US_ASCII);

    @Mock private TransactionAttachmentRepositoryPort attachmentRepositoryPort;
    @Mock private FileStoragePort fileStoragePort;
    @Mock private TransactionRepositoryPort transactionRepositoryPort;
    @Mock private AccountRepositoryPort accountRepositoryPort;

    @InjectMocks private TransactionAttachmentApplicationService service;

    @BeforeEach
    void setUp() {
        lenient()
                .when(transactionRepositoryPort.findById(TRANSACTION_ID))
                .thenReturn(Optional.of(transaction()));
        lenient()
                .when(accountRepositoryPort.findById(1L))
                .thenReturn(
                        Optional.of(
                                new Account(
                                        1L,
                                        USER_ID,
                                        "Conta",
                                        AccountType.CHECKING,
                                        BigDecimal.ZERO,
                                        null)));
        lenient()
                .when(attachmentRepositoryPort.findAllByTransactionId(TRANSACTION_ID))
                .thenReturn(List.of());
    }

    @Test
    void uploadStoresTheFileUnderAGeneratedKeyAndSavesTheRecord() {
        when(attachmentRepositoryPort.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TransactionAttachment saved =
                service.upload(
                        USER_ID,
                        TRANSACTION_ID,
                        AttachmentDocumentType.INVOICE,
                        "../../nota fiscal.pdf",
                        PDF);

        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(fileStoragePort).store(key.capture(), eq(PDF));
        assertThat(key.getValue()).matches("[0-9a-f-]{36}\\.pdf");
        assertThat(saved.storageKey()).isEqualTo(key.getValue());
        assertThat(saved.fileName()).isEqualTo("nota fiscal.pdf");
        assertThat(saved.contentType()).isEqualTo("application/pdf");
        assertThat(saved.sizeBytes()).isEqualTo(PDF.length);
        assertThat(saved.documentType()).isEqualTo(AttachmentDocumentType.INVOICE);
        assertThat(saved.userId()).isEqualTo(USER_ID);
    }

    @Test
    void rejectsEmptyTooLargeAndUnknownFormats() {
        assertThatThrownBy(() -> upload(new byte[0]))
                .isInstanceOf(AttachmentInvalidException.class);
        assertThatThrownBy(
                        () ->
                                upload(
                                        new byte
                                                [(int)
                                                                TransactionAttachmentApplicationService
                                                                        .MAX_SIZE_BYTES
                                                        + 1]))
                .isInstanceOf(AttachmentInvalidException.class)
                .hasMessageContaining("10 MB");
        // Executável renomeado para .pdf.
        assertThatThrownBy(() -> upload(new byte[] {0x4D, 0x5A, 0x00, 0x00}))
                .isInstanceOf(AttachmentInvalidException.class)
                .hasMessageContaining("Formato não aceito");
        verify(fileStoragePort, never()).store(anyString(), any());
    }

    @Test
    void rejectsMoreThanTheLimitPerTransaction() {
        when(attachmentRepositoryPort.findAllByTransactionId(TRANSACTION_ID))
                .thenReturn(
                        Collections.nCopies(
                                TransactionAttachmentApplicationService.MAX_PER_TRANSACTION,
                                attachment(1L, USER_ID)));

        assertThatThrownBy(() -> upload(PDF)).isInstanceOf(AttachmentInvalidException.class);
        verify(fileStoragePort, never()).store(anyString(), any());
    }

    @Test
    void removesTheStoredFileWhenSavingTheRecordFails() {
        when(attachmentRepositoryPort.save(any()))
                .thenThrow(new IllegalStateException("banco fora"));

        assertThatThrownBy(() -> upload(PDF)).isInstanceOf(IllegalStateException.class);

        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(fileStoragePort).store(key.capture(), eq(PDF));
        verify(fileStoragePort).delete(key.getValue());
    }

    @Test
    void transactionOfAnotherUserIsNotFound() {
        assertThatThrownBy(() -> service.list(99L, TRANSACTION_ID))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.upload(99L, TRANSACTION_ID, null, "a.pdf", PDF))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void attachmentMustBelongToTheInformedTransaction() {
        when(attachmentRepositoryPort.findById(5L))
                .thenReturn(
                        Optional.of(
                                new TransactionAttachment(
                                        5L,
                                        999L,
                                        USER_ID,
                                        AttachmentDocumentType.OTHER,
                                        "a.pdf",
                                        "application/pdf",
                                        10,
                                        "k.pdf",
                                        null)));

        assertThatThrownBy(() -> service.download(USER_ID, TRANSACTION_ID, 5L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteRemovesTheRecordAndTheFile() {
        when(attachmentRepositoryPort.findById(5L))
                .thenReturn(Optional.of(attachment(5L, USER_ID)));

        service.delete(USER_ID, TRANSACTION_ID, 5L);

        verify(attachmentRepositoryPort).deleteById(5L);
        verify(fileStoragePort).delete("chave-5.pdf");
    }

    @Test
    void sanitizesTheDisplayName() {
        assertThat(
                        TransactionAttachmentApplicationService.sanitizeFileName(
                                "C:\\fotos\\recibo<1>.png", AttachmentFileType.PNG))
                .isEqualTo("recibo1.png");
        assertThat(
                        TransactionAttachmentApplicationService.sanitizeFileName(
                                "comprovante", AttachmentFileType.PDF))
                .isEqualTo("comprovante.pdf");
        assertThat(
                        TransactionAttachmentApplicationService.sanitizeFileName(
                                null, AttachmentFileType.JPEG))
                .isEqualTo("anexo.jpg");
        assertThat(
                        TransactionAttachmentApplicationService.sanitizeFileName(
                                "foto.JPEG", AttachmentFileType.JPEG))
                .isEqualTo("foto.JPEG");
    }

    private TransactionAttachment upload(byte[] content) {
        return service.upload(
                USER_ID, TRANSACTION_ID, AttachmentDocumentType.RECEIPT, "a.pdf", content);
    }

    private static TransactionAttachment attachment(Long id, Long userId) {
        return new TransactionAttachment(
                id,
                TRANSACTION_ID,
                userId,
                AttachmentDocumentType.OTHER,
                "a.pdf",
                "application/pdf",
                10,
                "chave-" + id + ".pdf",
                null);
    }

    private static Transaction transaction() {
        return new Transaction(
                TRANSACTION_ID,
                1L,
                null,
                null,
                "Aluguel",
                BigDecimal.TEN,
                LocalDate.of(2026, 9, 1),
                CategoryType.EXPENSE,
                TransactionOrigin.MANUAL,
                null,
                null,
                null,
                null,
                TransactionStatus.PAID);
    }
}
