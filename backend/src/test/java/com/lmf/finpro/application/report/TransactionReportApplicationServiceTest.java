package com.lmf.finpro.application.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lmf.finpro.application.tag.TagApplicationService;
import com.lmf.finpro.domain.exception.ResourceNotFoundException;
import com.lmf.finpro.domain.model.Account;
import com.lmf.finpro.domain.model.AccountScope;
import com.lmf.finpro.domain.model.AccountType;
import com.lmf.finpro.domain.model.Category;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.Transaction;
import com.lmf.finpro.domain.model.TransactionOrigin;
import com.lmf.finpro.domain.model.TransactionReportData;
import com.lmf.finpro.domain.model.TransactionSearchCriteria;
import com.lmf.finpro.domain.model.TransactionStatus;
import com.lmf.finpro.domain.port.out.AccountRepositoryPort;
import com.lmf.finpro.domain.port.out.CategoryRepositoryPort;
import com.lmf.finpro.domain.port.out.ClientRepositoryPort;
import com.lmf.finpro.domain.port.out.ReceiptGeneratorPort;
import com.lmf.finpro.domain.port.out.ReportCsvExporterPort;
import com.lmf.finpro.domain.port.out.TransactionRepositoryPort;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransactionReportApplicationServiceTest {

    private static final Long USER_ID = 10L;

    @Mock private TransactionRepositoryPort transactionRepositoryPort;
    @Mock private AccountRepositoryPort accountRepositoryPort;
    @Mock private CategoryRepositoryPort categoryRepositoryPort;
    @Mock private ClientRepositoryPort clientRepositoryPort;
    @Mock private ReceiptGeneratorPort receiptGeneratorPort;
    @Mock private ReportCsvExporterPort reportCsvExporterPort;

    @Mock private TagApplicationService tagApplicationService;

    @InjectMocks private TransactionReportApplicationService service;

    @BeforeEach
    void setUp() {
        lenient()
                .when(accountRepositoryPort.findAllByUserId(USER_ID))
                .thenReturn(
                        List.of(
                                new Account(
                                        1L,
                                        USER_ID,
                                        "Nubank PJ",
                                        AccountType.CHECKING,
                                        BigDecimal.ZERO,
                                        null,
                                        AccountScope.BUSINESS)));
        lenient()
                .when(categoryRepositoryPort.findAllVisibleToUser(USER_ID))
                .thenReturn(
                        List.of(
                                new Category(
                                        5L, USER_ID, "Aluguel", CategoryType.EXPENSE, null, null),
                                new Category(
                                        6L, null, "Internet", CategoryType.EXPENSE, null, null)));
        lenient().when(clientRepositoryPort.findAllByUserId(USER_ID)).thenReturn(List.of());
        lenient().when(transactionRepositoryPort.search(any())).thenReturn(List.of());
    }

    @Test
    void nullFiltersReachTheSearchAsNull() {
        service.buildData(USER_ID, CategoryType.EXPENSE, noFilters());

        ArgumentCaptor<TransactionSearchCriteria> captor =
                ArgumentCaptor.forClass(TransactionSearchCriteria.class);
        verify(transactionRepositoryPort).search(captor.capture());
        TransactionSearchCriteria criteria = captor.getValue();
        assertThat(criteria.userId()).isEqualTo(USER_ID);
        assertThat(criteria.type()).isEqualTo(CategoryType.EXPENSE);
        assertThat(criteria.excludeTransfers()).isTrue();
        assertThat(criteria.startDate()).isNull();
        assertThat(criteria.accountId()).isNull();
        assertThat(criteria.categoryId()).isNull();
        assertThat(criteria.status()).isNull();
        assertThat(criteria.minAmount()).isNull();
        assertThat(criteria.description()).isNull();
    }

    @Test
    void buildsRowsTotalsAndCategorySubtotals() {
        when(transactionRepositoryPort.search(any()))
                .thenReturn(
                        List.of(
                                expense(5L, "1500", TransactionStatus.PAID),
                                expense(6L, "100", TransactionStatus.PENDING),
                                expense(6L, "120", TransactionStatus.PAID),
                                expense(null, "30", TransactionStatus.PAID)));

        TransactionReportData data = service.buildData(USER_ID, CategoryType.EXPENSE, noFilters());

        assertThat(data.rows()).hasSize(4);
        assertThat(data.rows().get(0).accountName()).isEqualTo("Nubank PJ");
        assertThat(data.total()).isEqualByComparingTo("1750");
        assertThat(data.paidTotal()).isEqualByComparingTo("1650");
        assertThat(data.pendingTotal()).isEqualByComparingTo("100");
        assertThat(data.categoryTotals())
                .extracting(TransactionReportData.CategoryTotal::categoryName)
                .containsExactly("Aluguel", "Internet", "Sem categoria");
        assertThat(data.categoryTotals().get(1).count()).isEqualTo(2);
        assertThat(data.periodLabel()).isEqualTo("Todo o período");
        assertThat(data.appliedFilters()).isEmpty();
    }

    @Test
    void describesEachInformedFilter() {
        TransactionReportFilters filters =
                new TransactionReportFilters(
                        LocalDate.of(2026, 9, 1),
                        LocalDate.of(2026, 9, 30),
                        1L,
                        AccountScope.BUSINESS,
                        5L,
                        null,
                        TransactionStatus.PENDING,
                        new BigDecimal("100"),
                        null,
                        " aluguel ");

        TransactionReportData data = service.buildData(USER_ID, CategoryType.EXPENSE, filters);

        assertThat(data.periodLabel()).isEqualTo("01/09/2026 a 30/09/2026");
        assertThat(data.appliedFilters())
                .containsExactly(
                        "Conta: Nubank PJ",
                        "Uso da conta: Empresa (PJ)",
                        "Categoria: Aluguel",
                        "Situação: Pendente",
                        "Valor mínimo: R$ 100,00",
                        "Descrição contém: \"aluguel\"");
    }

    @Test
    void rejectsInvertedPeriodAndAmounts() {
        TransactionReportFilters invertedDates =
                new TransactionReportFilters(
                        LocalDate.of(2026, 9, 30),
                        LocalDate.of(2026, 9, 1),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null);
        TransactionReportFilters invertedAmounts =
                new TransactionReportFilters(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        new BigDecimal("500"),
                        new BigDecimal("100"),
                        null);

        assertThatThrownBy(() -> service.buildData(USER_ID, CategoryType.INCOME, invertedDates))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.buildData(USER_ID, CategoryType.INCOME, invertedAmounts))
                .isInstanceOf(IllegalArgumentException.class);
        verify(transactionRepositoryPort, never()).search(any());
    }

    @Test
    void accountOfAnotherUserIsNotFound() {
        TransactionReportFilters filters =
                new TransactionReportFilters(
                        null, null, 999L, null, null, null, null, null, null, null);

        assertThatThrownBy(() -> service.buildData(USER_ID, CategoryType.INCOME, filters))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(transactionRepositoryPort, never()).search(any());
    }

    private static TransactionReportFilters noFilters() {
        return new TransactionReportFilters(
                null, null, null, null, null, null, null, null, null, null);
    }

    private static Transaction expense(Long categoryId, String amount, TransactionStatus status) {
        return new Transaction(
                null,
                1L,
                categoryId,
                null,
                "Despesa",
                new BigDecimal(amount),
                LocalDate.of(2026, 9, 10),
                CategoryType.EXPENSE,
                TransactionOrigin.MANUAL,
                null,
                null,
                null,
                null,
                status);
    }
}
