package com.lmf.finpro.application.attachment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.lmf.finpro.application.tag.TagApplicationService;
import com.lmf.finpro.domain.exception.ResourceNotFoundException;
import com.lmf.finpro.domain.model.Account;
import com.lmf.finpro.domain.model.AccountType;
import com.lmf.finpro.domain.model.AttachmentArchiveData;
import com.lmf.finpro.domain.model.AttachmentDocumentType;
import com.lmf.finpro.domain.model.Category;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.Transaction;
import com.lmf.finpro.domain.model.TransactionAttachment;
import com.lmf.finpro.domain.model.TransactionOrigin;
import com.lmf.finpro.domain.model.TransactionStatus;
import com.lmf.finpro.domain.port.out.AccountRepositoryPort;
import com.lmf.finpro.domain.port.out.AttachmentArchiveWriterPort;
import com.lmf.finpro.domain.port.out.CategoryRepositoryPort;
import com.lmf.finpro.domain.port.out.ClientRepositoryPort;
import com.lmf.finpro.domain.port.out.TransactionAttachmentRepositoryPort;
import com.lmf.finpro.domain.port.out.TransactionRepositoryPort;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AttachmentArchiveApplicationServiceTest {

    private static final Long USER_ID = 10L;

    @Mock private AccountRepositoryPort accountRepositoryPort;
    @Mock private TransactionRepositoryPort transactionRepositoryPort;
    @Mock private TransactionAttachmentRepositoryPort attachmentRepositoryPort;
    @Mock private CategoryRepositoryPort categoryRepositoryPort;
    @Mock private ClientRepositoryPort clientRepositoryPort;
    @Mock private AttachmentArchiveWriterPort attachmentArchiveWriterPort;

    @Mock private TagApplicationService tagApplicationService;

    @InjectMocks private AttachmentArchiveApplicationService service;

    @BeforeEach
    void setUp() {
        lenient()
                .when(accountRepositoryPort.findAllByHouseholdId(USER_ID))
                .thenReturn(
                        List.of(
                                new Account(
                                        1L,
                                        USER_ID,
                                        "Conta PJ",
                                        AccountType.CHECKING,
                                        BigDecimal.ZERO,
                                        null)));
        lenient()
                .when(transactionRepositoryPort.findAllByAccountIds(List.of(1L)))
                .thenReturn(
                        List.of(
                                tx(1L, "Café & Cia — março", LocalDate.of(2026, 3, 15), 5L),
                                tx(2L, "Aluguel", LocalDate.of(2026, 1, 10), null),
                                tx(3L, "Ano anterior", LocalDate.of(2025, 12, 31), null)));
        lenient()
                .when(categoryRepositoryPort.findAllVisibleToUser(USER_ID))
                .thenReturn(
                        List.of(
                                new Category(
                                        5L,
                                        USER_ID,
                                        "Alimentação",
                                        CategoryType.EXPENSE,
                                        null,
                                        null)));
        lenient().when(clientRepositoryPort.findAllByHouseholdId(USER_ID)).thenReturn(List.of());
    }

    @Test
    void entriesComeFromTheYearsTransactionsOrderedByDateInMonthFolders() {
        when(attachmentRepositoryPort.findAllByTransactionIds(Set.of(1L, 2L)))
                .thenReturn(
                        List.of(
                                attachment(20L, 1L, "application/pdf"),
                                attachment(21L, 2L, "image/png")));

        AttachmentArchiveData data = service.prepare(USER_ID, Year.of(2026));

        assertThat(data.entries())
                .extracting(AttachmentArchiveData.Entry::path)
                .containsExactly(
                        "2026-01/2026-01-10_aluguel_21.png",
                        "2026-03/2026-03-15_cafe-cia-marco_20.pdf");
        AttachmentArchiveData.Entry cafe = data.entries().get(1);
        assertThat(cafe.categoryName()).isEqualTo("Alimentação");
        assertThat(cafe.accountName()).isEqualTo("Conta PJ");
        assertThat(cafe.documentType()).isEqualTo(AttachmentDocumentType.RECEIPT);
        assertThat(data.entries().get(0).categoryName()).isEqualTo("Sem categoria");
    }

    @Test
    void yearWithoutAttachmentsIsNotFound() {
        when(attachmentRepositoryPort.findAllByTransactionIds(any())).thenReturn(List.of());

        assertThatThrownBy(() -> service.prepare(USER_ID, Year.of(2026)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("2026");
    }

    @Test
    void slugMakesSafeFileNames() {
        assertThat(AttachmentArchiveApplicationService.slug("Café & Cia — março"))
                .isEqualTo("cafe-cia-marco");
        assertThat(AttachmentArchiveApplicationService.slug("  ")).isEqualTo("transacao");
        assertThat(AttachmentArchiveApplicationService.slug("../../etc/passwd"))
                .isEqualTo("etc-passwd");
        assertThat(AttachmentArchiveApplicationService.slug("a".repeat(60))).hasSize(40);
    }

    private static TransactionAttachment attachment(
            Long id, Long transactionId, String contentType) {
        return new TransactionAttachment(
                id,
                transactionId,
                USER_ID,
                AttachmentDocumentType.RECEIPT,
                "original",
                contentType,
                10,
                "chave-" + id,
                null);
    }

    private static Transaction tx(Long id, String description, LocalDate date, Long categoryId) {
        return new Transaction(
                id,
                1L,
                categoryId,
                null,
                description,
                BigDecimal.TEN,
                date,
                CategoryType.EXPENSE,
                TransactionOrigin.MANUAL,
                null,
                null,
                null,
                null,
                TransactionStatus.PAID);
    }
}
