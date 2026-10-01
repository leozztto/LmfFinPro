package com.lmf.finpro.application.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.lmf.finpro.application.attachment.TransactionAttachmentApplicationService;
import com.lmf.finpro.application.exchangerate.ExchangeRateApplicationService;
import com.lmf.finpro.application.tag.TagApplicationService;
import com.lmf.finpro.domain.exception.CategoryTypeMismatchException;
import com.lmf.finpro.domain.exception.PaidTransactionLockedException;
import com.lmf.finpro.domain.exception.ResourceNotFoundException;
import com.lmf.finpro.domain.exception.TransactionLinkedToTransferException;
import com.lmf.finpro.domain.model.Account;
import com.lmf.finpro.domain.model.AccountScope;
import com.lmf.finpro.domain.model.AccountType;
import com.lmf.finpro.domain.model.Category;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.Client;
import com.lmf.finpro.domain.model.ClientWorkType;
import com.lmf.finpro.domain.model.Currency;
import com.lmf.finpro.domain.model.DocumentType;
import com.lmf.finpro.domain.model.PageQuery;
import com.lmf.finpro.domain.model.PageResult;
import com.lmf.finpro.domain.model.Tag;
import com.lmf.finpro.domain.model.Transaction;
import com.lmf.finpro.domain.model.TransactionOrigin;
import com.lmf.finpro.domain.model.TransactionSearchCriteria;
import com.lmf.finpro.domain.model.TransactionSortOrder;
import com.lmf.finpro.domain.model.TransactionStatus;
import com.lmf.finpro.domain.port.out.AccountRepositoryPort;
import com.lmf.finpro.domain.port.out.CategoryRepositoryPort;
import com.lmf.finpro.domain.port.out.ClientRepositoryPort;
import com.lmf.finpro.domain.port.out.TransactionRepositoryPort;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransactionApplicationServiceTest {

    @Mock private ExchangeRateApplicationService exchangeRateApplicationService;

    @Mock private TransactionRepositoryPort transactionRepositoryPort;
    @Mock private AccountRepositoryPort accountRepositoryPort;
    @Mock private CategoryRepositoryPort categoryRepositoryPort;
    @Mock private ClientRepositoryPort clientRepositoryPort;

    @Mock private TransactionAttachmentApplicationService transactionAttachmentApplicationService;
    @Mock private TagApplicationService tagApplicationService;

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 25);

    private TransactionApplicationService service;

    @BeforeEach
    void setUp() {
        Clock fixedClock =
                Clock.fixed(Instant.parse("2026-09-25T12:00:00Z"), ZoneId.of("America/Sao_Paulo"));
        service =
                new TransactionApplicationService(
                        transactionRepositoryPort,
                        accountRepositoryPort,
                        categoryRepositoryPort,
                        clientRepositoryPort,
                        fixedClock,
                        transactionAttachmentApplicationService,
                        tagApplicationService,
                        exchangeRateApplicationService);
    }

    private static Account ownedAccount() {
        return new Account(
                1L, 10L, "Conta", AccountType.CHECKING, BigDecimal.ZERO, LocalDateTime.now());
    }

    private static Transaction ownedTransaction(Long transferId) {
        return new Transaction(
                7L,
                1L,
                null,
                null,
                "Desc",
                BigDecimal.TEN,
                LocalDate.now(),
                CategoryType.EXPENSE,
                TransactionOrigin.MANUAL,
                LocalDateTime.now(),
                transferId,
                null);
    }

    @Test
    void createSucceedsWithOwnedAccountAndNoCategoryOrClient() {
        when(accountRepositoryPort.findById(1L)).thenReturn(Optional.of(ownedAccount()));
        when(transactionRepositoryPort.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Transaction created =
                service.create(
                        10L,
                        1L,
                        null,
                        null,
                        "Pagamento",
                        BigDecimal.valueOf(100),
                        LocalDate.now(),
                        CategoryType.EXPENSE,
                        null);

        assertThat(created.accountId()).isEqualTo(1L);
        assertThat(created.amount()).isEqualByComparingTo("100");
    }

    @Test
    void createInBrlAccountKeepsTheRealAmountAndTheOriginalForeignOperation() {
        when(accountRepositoryPort.findById(1L)).thenReturn(Optional.of(ownedAccount()));
        when(transactionRepositoryPort.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Transaction created =
                service.create(
                        10L,
                        1L,
                        null,
                        null,
                        "Hospedagem",
                        new BigDecimal("118.40"),
                        LocalDate.of(2026, 9, 18),
                        CategoryType.EXPENSE,
                        null,
                        List.of(),
                        Currency.USD,
                        new BigDecimal("20.00"));

        assertThat(created.amount()).isEqualByComparingTo("118.40");
        assertThat(created.baseAmount()).isEqualByComparingTo("118.40");
        assertThat(created.originalCurrency()).isEqualTo(Currency.USD);
        assertThat(created.originalAmount()).isEqualByComparingTo("20.00");
        verifyNoInteractions(exchangeRateApplicationService);
    }

    @Test
    void createInForeignAccountConvertsToRealAtTheRateOfTheDay() {
        Account usdAccount =
                new Account(
                        1L,
                        10L,
                        "Wise",
                        AccountType.CHECKING,
                        BigDecimal.ZERO,
                        LocalDateTime.now(),
                        AccountScope.BUSINESS,
                        Currency.USD);
        when(accountRepositoryPort.findById(1L)).thenReturn(Optional.of(usdAccount));
        when(exchangeRateApplicationService.toBrl(
                        Currency.USD, new BigDecimal("1000.00"), LocalDate.of(2026, 9, 18)))
                .thenReturn(new BigDecimal("5157.50"));
        when(transactionRepositoryPort.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Transaction created =
                service.create(
                        10L,
                        1L,
                        null,
                        null,
                        "Fatura cliente",
                        new BigDecimal("1000.00"),
                        LocalDate.of(2026, 9, 18),
                        CategoryType.INCOME,
                        null,
                        List.of(),
                        Currency.USD,
                        new BigDecimal("1000.00"));

        assertThat(created.baseAmount()).isEqualByComparingTo("5157.50");
        // Operação na própria moeda da conta: não é "moeda original".
        assertThat(created.originalCurrency()).isNull();
        assertThat(created.originalAmount()).isNull();
    }

    @Test
    void createWithForeignCurrencyRequiresTheOriginalAmount() {
        when(accountRepositoryPort.findById(1L)).thenReturn(Optional.of(ownedAccount()));

        assertThatThrownBy(
                        () ->
                                service.create(
                                        10L,
                                        1L,
                                        null,
                                        null,
                                        "Hospedagem",
                                        new BigDecimal("118.40"),
                                        LocalDate.of(2026, 9, 18),
                                        CategoryType.EXPENSE,
                                        null,
                                        List.of(),
                                        Currency.EUR,
                                        null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("EUR");
        verify(transactionRepositoryPort, never()).save(any());
    }

    @Test
    void createThrowsWhenCategoryDoesNotExist() {
        when(accountRepositoryPort.findById(1L)).thenReturn(Optional.of(ownedAccount()));
        when(categoryRepositoryPort.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                service.create(
                                        10L,
                                        1L,
                                        5L,
                                        null,
                                        "X",
                                        BigDecimal.TEN,
                                        LocalDate.now(),
                                        CategoryType.EXPENSE,
                                        null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createThrowsWhenAccountNotOwned() {
        when(accountRepositoryPort.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                service.create(
                                        10L,
                                        1L,
                                        null,
                                        null,
                                        "X",
                                        BigDecimal.TEN,
                                        LocalDate.now(),
                                        CategoryType.EXPENSE,
                                        null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createThrowsWhenCategoryTypeDoesNotMatchTransactionType() {
        when(accountRepositoryPort.findById(1L)).thenReturn(Optional.of(ownedAccount()));
        Category incomeCategory = new Category(5L, 10L, "Salário", CategoryType.INCOME, null, null);
        when(categoryRepositoryPort.findById(5L)).thenReturn(Optional.of(incomeCategory));

        assertThatThrownBy(
                        () ->
                                service.create(
                                        10L,
                                        1L,
                                        5L,
                                        null,
                                        "X",
                                        BigDecimal.TEN,
                                        LocalDate.now(),
                                        CategoryType.EXPENSE,
                                        null))
                .isInstanceOf(CategoryTypeMismatchException.class);
    }

    @Test
    void createThrowsWhenClientNotOwned() {
        when(accountRepositoryPort.findById(1L)).thenReturn(Optional.of(ownedAccount()));
        when(clientRepositoryPort.findById(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                service.create(
                                        10L,
                                        1L,
                                        null,
                                        3L,
                                        "X",
                                        BigDecimal.TEN,
                                        LocalDate.now(),
                                        CategoryType.INCOME,
                                        null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createSucceedsWithOwnedClient() {
        when(accountRepositoryPort.findById(1L)).thenReturn(Optional.of(ownedAccount()));
        Client ownedClient =
                Client.create(
                        10L,
                        "Cliente",
                        null,
                        null,
                        DocumentType.CPF,
                        "52998224725",
                        ClientWorkType.PJ,
                        null,
                        null,
                        true);
        when(clientRepositoryPort.findById(3L)).thenReturn(Optional.of(ownedClient));
        when(transactionRepositoryPort.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Transaction created =
                service.create(
                        10L,
                        1L,
                        null,
                        3L,
                        "X",
                        BigDecimal.TEN,
                        LocalDate.now(),
                        CategoryType.INCOME,
                        null);

        assertThat(created.clientId()).isEqualTo(3L);
    }

    private static TransactionListFilters noFilters() {
        return new TransactionListFilters(
                null, null, null, null, null, null, List.of(), null, null);
    }

    @Test
    void listSearchesOnlyTheCurrentUsersTransactionsWithDefaultPaging() {
        when(transactionRepositoryPort.searchPage(any(), any(), any()))
                .thenReturn(new PageResult<>(List.of(ownedTransaction(null)), 0, 20, 1));

        PageResult<Transaction> page = service.list(10L, noFilters(), null, null);

        ArgumentCaptor<TransactionSearchCriteria> criteria =
                ArgumentCaptor.forClass(TransactionSearchCriteria.class);
        verify(transactionRepositoryPort)
                .searchPage(
                        criteria.capture(),
                        eq(new PageQuery(0, 20)),
                        eq(TransactionSortOrder.NEWEST_FIRST));
        assertThat(criteria.getValue().userId()).isEqualTo(10L);
        assertThat(criteria.getValue().excludeTransfers()).isFalse();
        assertThat(page.content()).hasSize(1);
    }

    @Test
    void listClampsPageAndSize() {
        when(transactionRepositoryPort.searchPage(any(), any(), any()))
                .thenReturn(PageResult.empty(0, 100));

        service.list(10L, noFilters(), -3, 5000);

        verify(transactionRepositoryPort)
                .searchPage(
                        any(), eq(new PageQuery(0, 100)), eq(TransactionSortOrder.NEWEST_FIRST));
    }

    @Test
    void listPassesFiltersToTheSearch() {
        when(transactionRepositoryPort.searchPage(any(), any(), any()))
                .thenReturn(PageResult.empty(0, 20));
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 1, 31);

        service.list(
                10L,
                new TransactionListFilters(
                        1L,
                        2L,
                        3L,
                        CategoryType.INCOME,
                        TransactionStatus.PENDING,
                        true,
                        List.of(),
                        start,
                        end),
                null,
                null);

        ArgumentCaptor<TransactionSearchCriteria> criteria =
                ArgumentCaptor.forClass(TransactionSearchCriteria.class);
        verify(transactionRepositoryPort).searchPage(criteria.capture(), any(), any());
        TransactionSearchCriteria value = criteria.getValue();
        assertThat(value.accountId()).isEqualTo(1L);
        assertThat(value.categoryId()).isEqualTo(2L);
        assertThat(value.clientId()).isEqualTo(3L);
        assertThat(value.type()).isEqualTo(CategoryType.INCOME);
        assertThat(value.status()).isEqualTo(TransactionStatus.PENDING);
        assertThat(value.hasAttachment()).isTrue();
        assertThat(value.startDate()).isEqualTo(start);
        assertThat(value.endDate()).isEqualTo(end);
    }

    @Test
    void listResolvesTagNamesToTheUsersTagIds() {
        Tag tag = new Tag(5L, 10L, "projeto-acme", null, LocalDateTime.now());
        Tag other = new Tag(6L, 10L, "outra", null, LocalDateTime.now());
        when(tagApplicationService.tagsById(10L)).thenReturn(Map.of(5L, tag, 6L, other));
        when(transactionRepositoryPort.searchPage(any(), any(), any()))
                .thenReturn(PageResult.empty(0, 20));

        service.list(
                10L,
                new TransactionListFilters(
                        null, null, null, null, null, null, List.of("#Projeto-Acme"), null, null),
                null,
                null);

        ArgumentCaptor<TransactionSearchCriteria> criteria =
                ArgumentCaptor.forClass(TransactionSearchCriteria.class);
        verify(transactionRepositoryPort).searchPage(criteria.capture(), any(), any());
        assertThat(criteria.getValue().tagIds()).containsExactly(5L);
    }

    @Test
    void listReturnsEmptyPageWithoutQueryingWhenNoRequestedTagExists() {
        when(tagApplicationService.tagsById(10L)).thenReturn(Map.of());

        PageResult<Transaction> page =
                service.list(
                        10L,
                        new TransactionListFilters(
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                List.of("fantasma"),
                                null,
                                null),
                        null,
                        null);

        assertThat(page.content()).isEmpty();
        verify(transactionRepositoryPort, never()).searchPage(any(), any(), any());
    }

    @Test
    void getByIdThrowsWhenTransactionsAccountBelongsToAnotherUser() {
        when(transactionRepositoryPort.findById(7L))
                .thenReturn(Optional.of(ownedTransaction(null)));
        when(accountRepositoryPort.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(10L, 7L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getByIdThrowsWhenTransactionDoesNotExist() {
        when(transactionRepositoryPort.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(10L, 7L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getByIdReturnsOwnedTransaction() {
        when(transactionRepositoryPort.findById(7L))
                .thenReturn(Optional.of(ownedTransaction(null)));
        when(accountRepositoryPort.findById(1L)).thenReturn(Optional.of(ownedAccount()));

        Transaction result = service.getById(10L, 7L);

        assertThat(result.id()).isEqualTo(7L);
    }

    @Test
    void deleteThrowsWhenTransactionIsPartOfATransfer() {
        when(transactionRepositoryPort.findById(7L)).thenReturn(Optional.of(ownedTransaction(50L)));
        when(accountRepositoryPort.findById(1L)).thenReturn(Optional.of(ownedAccount()));

        assertThatThrownBy(() -> service.delete(10L, 7L))
                .isInstanceOf(TransactionLinkedToTransferException.class);
        verify(transactionRepositoryPort, never()).deleteById(any());
    }

    @Test
    void deleteRemovesStandaloneTransaction() {
        when(transactionRepositoryPort.findById(7L))
                .thenReturn(Optional.of(ownedTransaction(null)));
        when(accountRepositoryPort.findById(1L)).thenReturn(Optional.of(ownedAccount()));

        service.delete(10L, 7L);

        verify(transactionRepositoryPort).deleteById(7L);
    }

    private Transaction createWithStatus(LocalDate date, TransactionStatus status) {
        when(accountRepositoryPort.findById(1L)).thenReturn(Optional.of(ownedAccount()));
        when(transactionRepositoryPort.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        return service.create(
                10L, 1L, null, null, "X", BigDecimal.TEN, date, CategoryType.EXPENSE, status);
    }

    @Test
    void createWithoutStatusMarksFutureDateAsPending() {
        assertThat(createWithStatus(TODAY.plusDays(1), null).status())
                .isEqualTo(TransactionStatus.PENDING);
    }

    @Test
    void createWithoutStatusMarksTodayAsPaid() {
        assertThat(createWithStatus(TODAY, null).status()).isEqualTo(TransactionStatus.PAID);
    }

    @Test
    void createKeepsExplicitStatusEvenIfDateSuggestsOtherwise() {
        assertThat(createWithStatus(TODAY.minusDays(3), TransactionStatus.PENDING).status())
                .isEqualTo(TransactionStatus.PENDING);
    }

    @Test
    void updateStatusMarksTransactionAsPaid() {
        when(transactionRepositoryPort.findById(7L))
                .thenReturn(
                        Optional.of(ownedTransaction(null).withStatus(TransactionStatus.PENDING)));
        when(accountRepositoryPort.findById(1L)).thenReturn(Optional.of(ownedAccount()));
        when(transactionRepositoryPort.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Transaction updated = service.updateStatus(10L, 7L, TransactionStatus.PAID);

        assertThat(updated.status()).isEqualTo(TransactionStatus.PAID);
    }

    @Test
    void updateStatusRejectsPendingForTransferTransactions() {
        when(transactionRepositoryPort.findById(7L)).thenReturn(Optional.of(ownedTransaction(50L)));
        when(accountRepositoryPort.findById(1L)).thenReturn(Optional.of(ownedAccount()));

        assertThatThrownBy(() -> service.updateStatus(10L, 7L, TransactionStatus.PENDING))
                .isInstanceOf(TransactionLinkedToTransferException.class);
        verify(transactionRepositoryPort, never()).save(any());
    }

    @Test
    void updateWithoutStatusKeepsCurrentStatus() {
        when(transactionRepositoryPort.findById(7L))
                .thenReturn(
                        Optional.of(ownedTransaction(null).withStatus(TransactionStatus.PENDING)));
        when(accountRepositoryPort.findById(1L)).thenReturn(Optional.of(ownedAccount()));
        when(transactionRepositoryPort.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.update(
                10L,
                7L,
                null,
                null,
                "Novo",
                BigDecimal.ONE,
                TODAY,
                CategoryType.EXPENSE,
                null,
                null);

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepositoryPort).save(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(TransactionStatus.PENDING);
        assertThat(captor.getValue().description()).isEqualTo("Novo");
    }

    @Test
    void updateCanMarkAsPaidTogetherWithDetails() {
        when(transactionRepositoryPort.findById(7L))
                .thenReturn(
                        Optional.of(ownedTransaction(null).withStatus(TransactionStatus.PENDING)));
        when(accountRepositoryPort.findById(1L)).thenReturn(Optional.of(ownedAccount()));
        when(transactionRepositoryPort.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Transaction updated =
                service.update(
                        10L,
                        7L,
                        null,
                        null,
                        "Desc",
                        BigDecimal.TEN,
                        TODAY,
                        CategoryType.EXPENSE,
                        TransactionStatus.PAID,
                        null);

        assertThat(updated.status()).isEqualTo(TransactionStatus.PAID);
    }

    @Test
    void paidTransactionCannotGoBackToPendingByEditOrQuickAction() {
        when(transactionRepositoryPort.findById(7L))
                .thenReturn(Optional.of(ownedTransaction(null)));
        when(accountRepositoryPort.findById(1L)).thenReturn(Optional.of(ownedAccount()));

        assertThatThrownBy(() -> service.updateStatus(10L, 7L, TransactionStatus.PENDING))
                .isInstanceOf(PaidTransactionLockedException.class);
        assertThatThrownBy(
                        () ->
                                service.update(
                                        10L,
                                        7L,
                                        null,
                                        null,
                                        "Desc",
                                        BigDecimal.TEN,
                                        TODAY,
                                        CategoryType.EXPENSE,
                                        TransactionStatus.PENDING,
                                        null))
                .isInstanceOf(PaidTransactionLockedException.class);
        verify(transactionRepositoryPort, never()).save(any());
    }

    @Test
    void markingAPaidTransactionAsPaidAgainIsHarmless() {
        when(transactionRepositoryPort.findById(7L))
                .thenReturn(Optional.of(ownedTransaction(null)));
        when(accountRepositoryPort.findById(1L)).thenReturn(Optional.of(ownedAccount()));
        when(transactionRepositoryPort.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.updateStatus(10L, 7L, TransactionStatus.PAID).status())
                .isEqualTo(TransactionStatus.PAID);
    }
}
