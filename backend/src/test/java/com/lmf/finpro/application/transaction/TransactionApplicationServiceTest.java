package com.lmf.finpro.application.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lmf.finpro.domain.exception.CategoryTypeMismatchException;
import com.lmf.finpro.domain.exception.ResourceNotFoundException;
import com.lmf.finpro.domain.exception.TransactionLinkedToTransferException;
import com.lmf.finpro.domain.model.Account;
import com.lmf.finpro.domain.model.AccountType;
import com.lmf.finpro.domain.model.Category;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.Client;
import com.lmf.finpro.domain.model.ClientWorkType;
import com.lmf.finpro.domain.model.DocumentType;
import com.lmf.finpro.domain.model.Transaction;
import com.lmf.finpro.domain.model.TransactionOrigin;
import com.lmf.finpro.domain.port.out.AccountRepositoryPort;
import com.lmf.finpro.domain.port.out.CategoryRepositoryPort;
import com.lmf.finpro.domain.port.out.ClientRepositoryPort;
import com.lmf.finpro.domain.port.out.TransactionRepositoryPort;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransactionApplicationServiceTest {

    @Mock private TransactionRepositoryPort transactionRepositoryPort;
    @Mock private AccountRepositoryPort accountRepositoryPort;
    @Mock private CategoryRepositoryPort categoryRepositoryPort;
    @Mock private ClientRepositoryPort clientRepositoryPort;

    @InjectMocks private TransactionApplicationService service;

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
                        CategoryType.EXPENSE);

        assertThat(created.accountId()).isEqualTo(1L);
        assertThat(created.amount()).isEqualByComparingTo("100");
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
                                        CategoryType.EXPENSE))
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
                                        CategoryType.EXPENSE))
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
                                        CategoryType.EXPENSE))
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
                                        CategoryType.INCOME))
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
                        CategoryType.INCOME);

        assertThat(created.clientId()).isEqualTo(3L);
    }

    @Test
    void listReturnsTransactionsForAllOwnedAccounts() {
        when(accountRepositoryPort.findAllByUserId(10L)).thenReturn(List.of(ownedAccount()));
        when(transactionRepositoryPort.findAllByAccountIds(List.of(1L)))
                .thenReturn(List.of(ownedTransaction(null)));

        assertThat(service.list(10L)).hasSize(1);
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
}
