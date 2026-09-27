package com.lmf.finpro.application.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lmf.finpro.domain.exception.EntityHasLinkedRecordsException;
import com.lmf.finpro.domain.exception.ResourceNotFoundException;
import com.lmf.finpro.domain.model.Account;
import com.lmf.finpro.domain.model.AccountScope;
import com.lmf.finpro.domain.model.AccountType;
import com.lmf.finpro.domain.model.AccountValuation;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.Transaction;
import com.lmf.finpro.domain.model.TransactionOrigin;
import com.lmf.finpro.domain.port.out.AccountRepositoryPort;
import com.lmf.finpro.domain.port.out.AccountValuationRepositoryPort;
import com.lmf.finpro.domain.port.out.RecurringTransactionRepositoryPort;
import com.lmf.finpro.domain.port.out.TransactionRepositoryPort;
import com.lmf.finpro.domain.port.out.TransferRepositoryPort;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountApplicationServiceTest {

    @Mock private AccountRepositoryPort accountRepositoryPort;
    @Mock private TransactionRepositoryPort transactionRepositoryPort;
    @Mock private TransferRepositoryPort transferRepositoryPort;
    @Mock private RecurringTransactionRepositoryPort recurringTransactionRepositoryPort;
    @Mock private AccountValuationRepositoryPort accountValuationRepositoryPort;

    @InjectMocks private AccountApplicationService service;

    private Account existingAccount;

    @BeforeEach
    void setUp() {
        existingAccount =
                new Account(
                        1L,
                        10L,
                        "Conta Corrente",
                        AccountType.CHECKING,
                        BigDecimal.valueOf(1000),
                        LocalDateTime.now());
    }

    @Test
    void createSavesAccountBuiltFromInput() {
        when(accountRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Account created =
                service.create(
                        10L,
                        "Carteira",
                        AccountType.WALLET,
                        BigDecimal.ZERO,
                        AccountScope.BUSINESS);

        assertThat(created.userId()).isEqualTo(10L);
        assertThat(created.name()).isEqualTo("Carteira");
        assertThat(created.type()).isEqualTo(AccountType.WALLET);
        assertThat(created.scope()).isEqualTo(AccountScope.BUSINESS);
    }

    @Test
    void createWithoutScopeIsPersonal() {
        when(accountRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Account created =
                service.create(10L, "Carteira", AccountType.WALLET, BigDecimal.ZERO, null);

        assertThat(created.scope()).isEqualTo(AccountScope.PERSONAL);
    }

    @Test
    void listReturnsAllAccountsForUser() {
        when(accountRepositoryPort.findAllByUserId(10L)).thenReturn(List.of(existingAccount));

        assertThat(service.list(10L)).containsExactly(existingAccount);
    }

    @Test
    void getByIdReturnsAccountWhenOwned() {
        when(accountRepositoryPort.findById(1L)).thenReturn(Optional.of(existingAccount));

        assertThat(service.getById(10L, 1L)).isEqualTo(existingAccount);
    }

    @Test
    void getByIdThrowsWhenAccountDoesNotExist() {
        when(accountRepositoryPort.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(10L, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getByIdThrowsWhenAccountBelongsToAnotherUser() {
        when(accountRepositoryPort.findById(1L)).thenReturn(Optional.of(existingAccount));

        assertThatThrownBy(() -> service.getById(999L, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateKeepsOriginalInitialBalanceRegardlessOfRequestedValue() {
        when(accountRepositoryPort.findById(1L)).thenReturn(Optional.of(existingAccount));
        when(accountRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Account updated =
                service.update(10L, 1L, "Renomeada", AccountType.SAVINGS, AccountScope.BUSINESS);

        assertThat(updated.name()).isEqualTo("Renomeada");
        assertThat(updated.type()).isEqualTo(AccountType.SAVINGS);
        assertThat(updated.initialBalance()).isEqualByComparingTo("1000");
        assertThat(updated.scope()).isEqualTo(AccountScope.BUSINESS);
    }

    @Test
    void updateWithoutScopeKeepsTheCurrentOne() {
        Account business =
                existingAccount.withDetails(
                        existingAccount.name(),
                        existingAccount.type(),
                        existingAccount.initialBalance(),
                        AccountScope.BUSINESS);
        when(accountRepositoryPort.findById(1L)).thenReturn(Optional.of(business));
        when(accountRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Account updated = service.update(10L, 1L, "Renomeada", AccountType.SAVINGS, null);

        assertThat(updated.scope()).isEqualTo(AccountScope.BUSINESS);
    }

    @Test
    void investmentBalanceFollowsTheLatestMarketValue() {
        Account investment =
                new Account(3L, 10L, "Corretora", AccountType.INVESTMENT, BigDecimal.ZERO, null);
        when(accountValuationRepositoryPort.findAllByAccountId(3L))
                .thenReturn(
                        List.of(
                                new AccountValuation(
                                        1L,
                                        3L,
                                        LocalDate.of(2026, 9, 1),
                                        new BigDecimal("1100"),
                                        null)));
        when(transactionRepositoryPort.findAllByAccountIds(List.of(3L)))
                .thenReturn(
                        List.of(
                                new Transaction(
                                        null,
                                        3L,
                                        null,
                                        null,
                                        "Aplicação",
                                        new BigDecimal("1000"),
                                        LocalDate.of(2026, 8, 10),
                                        CategoryType.INCOME,
                                        TransactionOrigin.MANUAL,
                                        null,
                                        50L,
                                        null)));

        assertThat(service.calculateCurrentBalance(investment)).isEqualByComparingTo("1100");
    }

    @Test
    void investmentWithMarketValuesCannotChangeItsType() {
        Account investment =
                new Account(1L, 10L, "Corretora", AccountType.INVESTMENT, BigDecimal.ZERO, null);
        when(accountRepositoryPort.findById(1L)).thenReturn(Optional.of(investment));
        when(accountValuationRepositoryPort.existsByAccountId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.update(10L, 1L, "Corretora", AccountType.CHECKING, null))
                .isInstanceOf(EntityHasLinkedRecordsException.class);
        verify(accountRepositoryPort, never()).save(any());
    }

    @Test
    void updateThrowsWhenAccountNotOwned() {
        when(accountRepositoryPort.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(10L, 1L, "X", AccountType.CHECKING, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteRemovesAccountWhenNoLinkedRecords() {
        when(accountRepositoryPort.findById(1L)).thenReturn(Optional.of(existingAccount));
        when(transactionRepositoryPort.existsByAccountId(1L)).thenReturn(false);
        when(transferRepositoryPort.existsByAccountId(1L)).thenReturn(false);

        service.delete(10L, 1L);

        verify(accountRepositoryPort).deleteById(1L);
    }

    @Test
    void deleteThrowsWhenAccountHasLinkedTransactions() {
        when(accountRepositoryPort.findById(1L)).thenReturn(Optional.of(existingAccount));
        when(transactionRepositoryPort.existsByAccountId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(10L, 1L))
                .isInstanceOf(EntityHasLinkedRecordsException.class);
        verify(accountRepositoryPort, never()).deleteById(any());
    }

    @Test
    void deleteThrowsWhenAccountHasLinkedRecurringTransactions() {
        when(accountRepositoryPort.findById(1L)).thenReturn(Optional.of(existingAccount));
        when(transactionRepositoryPort.existsByAccountId(1L)).thenReturn(false);
        when(transferRepositoryPort.existsByAccountId(1L)).thenReturn(false);
        when(recurringTransactionRepositoryPort.existsByAccountId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(10L, 1L))
                .isInstanceOf(EntityHasLinkedRecordsException.class);
        verify(accountRepositoryPort, never()).deleteById(any());
    }

    @Test
    void deleteThrowsWhenAccountHasLinkedTransfers() {
        when(accountRepositoryPort.findById(1L)).thenReturn(Optional.of(existingAccount));
        when(transactionRepositoryPort.existsByAccountId(1L)).thenReturn(false);
        when(transferRepositoryPort.existsByAccountId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(10L, 1L))
                .isInstanceOf(EntityHasLinkedRecordsException.class);
        verify(accountRepositoryPort, never()).deleteById(any());
    }

    @Test
    void calculateCurrentBalanceAddsIncomeAndSubtractsExpenseFromInitialBalance() {
        when(transactionRepositoryPort.sumPaidAmountByAccountIdAndType(1L, CategoryType.INCOME))
                .thenReturn(BigDecimal.valueOf(500));
        when(transactionRepositoryPort.sumPaidAmountByAccountIdAndType(1L, CategoryType.EXPENSE))
                .thenReturn(BigDecimal.valueOf(200));

        BigDecimal balance = service.calculateCurrentBalance(existingAccount);

        assertThat(balance).isEqualByComparingTo("1300"); // 1000 + 500 - 200
    }
}
