package com.lmf.finpro.application.recurringtransaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lmf.finpro.domain.exception.CategoryTypeMismatchException;
import com.lmf.finpro.domain.exception.InvalidRecurrencePeriodException;
import com.lmf.finpro.domain.exception.ResourceNotFoundException;
import com.lmf.finpro.domain.model.Account;
import com.lmf.finpro.domain.model.AccountType;
import com.lmf.finpro.domain.model.Category;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.RecurrenceFrequency;
import com.lmf.finpro.domain.model.RecurringTransaction;
import com.lmf.finpro.domain.model.Transaction;
import com.lmf.finpro.domain.model.TransactionOrigin;
import com.lmf.finpro.domain.model.TransactionStatus;
import com.lmf.finpro.domain.port.out.AccountRepositoryPort;
import com.lmf.finpro.domain.port.out.CategoryRepositoryPort;
import com.lmf.finpro.domain.port.out.ClientRepositoryPort;
import com.lmf.finpro.domain.port.out.RecurringTransactionRepositoryPort;
import com.lmf.finpro.domain.port.out.TransactionRepositoryPort;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecurringTransactionApplicationServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 25);

    @Mock private RecurringTransactionRepositoryPort recurringTransactionRepositoryPort;
    @Mock private TransactionRepositoryPort transactionRepositoryPort;
    @Mock private AccountRepositoryPort accountRepositoryPort;
    @Mock private CategoryRepositoryPort categoryRepositoryPort;
    @Mock private ClientRepositoryPort clientRepositoryPort;

    private RecurringTransactionApplicationService service;

    @BeforeEach
    void setUp() {
        Clock fixedClock =
                Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneId.of("UTC"));
        service =
                new RecurringTransactionApplicationService(
                        recurringTransactionRepositoryPort,
                        transactionRepositoryPort,
                        accountRepositoryPort,
                        categoryRepositoryPort,
                        clientRepositoryPort,
                        fixedClock);
    }

    private Account ownedAccount() {
        return new Account(2L, 10L, "Conta", AccountType.CHECKING, BigDecimal.ZERO, null);
    }

    private RecurringTransaction existing(int generatedOccurrences, boolean active) {
        return new RecurringTransaction(
                1L,
                10L,
                2L,
                null,
                null,
                "Aluguel",
                BigDecimal.valueOf(1500),
                CategoryType.EXPENSE,
                RecurrenceFrequency.MONTHLY,
                LocalDate.of(2026, 7, 5),
                null,
                generatedOccurrences,
                active,
                LocalDateTime.now());
    }

    @Test
    void createWithPastStartDateGeneratesEveryDueOccurrenceImmediately() {
        when(accountRepositoryPort.findById(2L)).thenReturn(Optional.of(ownedAccount()));
        when(recurringTransactionRepositoryPort.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RecurringTransaction created =
                service.create(
                        10L,
                        2L,
                        null,
                        null,
                        "Aluguel",
                        BigDecimal.valueOf(1500),
                        CategoryType.EXPENSE,
                        RecurrenceFrequency.MONTHLY,
                        LocalDate.of(2026, 7, 5),
                        null);

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepositoryPort, times(3)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(Transaction::transactionDate)
                .containsExactly(
                        LocalDate.of(2026, 7, 5),
                        LocalDate.of(2026, 8, 5),
                        LocalDate.of(2026, 9, 5));
        assertThat(captor.getAllValues())
                .allSatisfy(
                        transaction -> {
                            assertThat(transaction.origin()).isEqualTo(TransactionOrigin.RECURRING);
                            assertThat(transaction.status()).isEqualTo(TransactionStatus.PENDING);
                            assertThat(transaction.accountId()).isEqualTo(2L);
                            assertThat(transaction.amount()).isEqualByComparingTo("1500");
                        });
        assertThat(created.generatedOccurrences()).isEqualTo(3);
        assertThat(created.nextOccurrenceDate()).isEqualTo(LocalDate.of(2026, 10, 5));
    }

    @Test
    void createWithFutureStartDateDoesNotGenerateTransactions() {
        when(accountRepositoryPort.findById(2L)).thenReturn(Optional.of(ownedAccount()));
        when(recurringTransactionRepositoryPort.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RecurringTransaction created =
                service.create(
                        10L,
                        2L,
                        null,
                        null,
                        "Assinatura",
                        BigDecimal.valueOf(50),
                        CategoryType.EXPENSE,
                        RecurrenceFrequency.MONTHLY,
                        LocalDate.of(2026, 10, 1),
                        null);

        verify(transactionRepositoryPort, never()).save(any());
        assertThat(created.generatedOccurrences()).isZero();
    }

    @Test
    void createThrowsWhenAccountBelongsToAnotherUser() {
        when(accountRepositoryPort.findById(2L)).thenReturn(Optional.of(ownedAccount()));

        assertThatThrownBy(
                        () ->
                                service.create(
                                        999L,
                                        2L,
                                        null,
                                        null,
                                        "Aluguel",
                                        BigDecimal.TEN,
                                        CategoryType.EXPENSE,
                                        RecurrenceFrequency.MONTHLY,
                                        TODAY,
                                        null))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(recurringTransactionRepositoryPort, never()).save(any());
    }

    @Test
    void createThrowsWhenCategoryTypeDoesNotMatch() {
        when(accountRepositoryPort.findById(2L)).thenReturn(Optional.of(ownedAccount()));
        when(categoryRepositoryPort.findById(7L))
                .thenReturn(
                        Optional.of(
                                new Category(7L, 10L, "Salário", CategoryType.INCOME, null, null)));

        assertThatThrownBy(
                        () ->
                                service.create(
                                        10L,
                                        2L,
                                        7L,
                                        null,
                                        "Aluguel",
                                        BigDecimal.TEN,
                                        CategoryType.EXPENSE,
                                        RecurrenceFrequency.MONTHLY,
                                        TODAY,
                                        null))
                .isInstanceOf(CategoryTypeMismatchException.class);
    }

    @Test
    void createThrowsWhenEndDateIsBeforeStartDate() {
        when(accountRepositoryPort.findById(2L)).thenReturn(Optional.of(ownedAccount()));

        assertThatThrownBy(
                        () ->
                                service.create(
                                        10L,
                                        2L,
                                        null,
                                        null,
                                        "Aluguel",
                                        BigDecimal.TEN,
                                        CategoryType.EXPENSE,
                                        RecurrenceFrequency.MONTHLY,
                                        TODAY,
                                        TODAY.minusDays(1)))
                .isInstanceOf(InvalidRecurrencePeriodException.class);
    }

    @Test
    void listReturnsRecurrencesOfUser() {
        RecurringTransaction recurrence = existing(0, true);
        when(recurringTransactionRepositoryPort.findAllByUserId(10L))
                .thenReturn(List.of(recurrence));

        assertThat(service.list(10L)).containsExactly(recurrence);
    }

    @Test
    void updateChangesDetailsWithoutGeneratingWhenNothingIsDue() {
        when(recurringTransactionRepositoryPort.findById(1L))
                .thenReturn(Optional.of(existing(3, true)));
        when(recurringTransactionRepositoryPort.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RecurringTransaction updated =
                service.update(
                        10L, 1L, null, null, "Aluguel novo", BigDecimal.valueOf(1600), null, true);

        assertThat(updated.description()).isEqualTo("Aluguel novo");
        assertThat(updated.amount()).isEqualByComparingTo("1600");
        verify(transactionRepositoryPort, never()).save(any());
    }

    @Test
    void updateThrowsWhenRecurrenceBelongsToAnotherUser() {
        when(recurringTransactionRepositoryPort.findById(1L))
                .thenReturn(Optional.of(existing(0, true)));

        assertThatThrownBy(
                        () -> service.update(999L, 1L, null, null, "X", BigDecimal.TEN, null, true))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteRemovesOwnedRecurrence() {
        when(recurringTransactionRepositoryPort.findById(1L))
                .thenReturn(Optional.of(existing(0, true)));

        service.delete(10L, 1L);

        verify(recurringTransactionRepositoryPort).deleteById(1L);
    }

    @Test
    void deleteThrowsWhenRecurrenceDoesNotExist() {
        when(recurringTransactionRepositoryPort.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(10L, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(recurringTransactionRepositoryPort, never()).deleteById(any());
    }

    @Test
    void generateDueOccurrencesOnlyLaunchesMissingOnes() {
        when(recurringTransactionRepositoryPort.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RecurringTransaction result = service.generateDueOccurrences(existing(2, true));

        verify(transactionRepositoryPort, times(1)).save(any());
        assertThat(result.generatedOccurrences()).isEqualTo(3);
    }

    @Test
    void findAllActiveDelegatesToRepository() {
        RecurringTransaction recurrence = existing(0, true);
        when(recurringTransactionRepositoryPort.findAllActive()).thenReturn(List.of(recurrence));

        assertThat(service.findAllActive()).containsExactly(recurrence);
    }
}
