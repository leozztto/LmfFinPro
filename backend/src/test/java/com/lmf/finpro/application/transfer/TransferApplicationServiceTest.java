package com.lmf.finpro.application.transfer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lmf.finpro.application.account.AccountApplicationService;
import com.lmf.finpro.domain.exception.InsufficientBalanceException;
import com.lmf.finpro.domain.exception.ResourceNotFoundException;
import com.lmf.finpro.domain.exception.SameAccountTransferException;
import com.lmf.finpro.domain.model.Account;
import com.lmf.finpro.domain.model.AccountType;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.Transaction;
import com.lmf.finpro.domain.model.Transfer;
import com.lmf.finpro.domain.port.out.AccountRepositoryPort;
import com.lmf.finpro.domain.port.out.TransactionRepositoryPort;
import com.lmf.finpro.domain.port.out.TransferRepositoryPort;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransferApplicationServiceTest {

    @Mock private TransferRepositoryPort transferRepositoryPort;
    @Mock private TransactionRepositoryPort transactionRepositoryPort;
    @Mock private AccountRepositoryPort accountRepositoryPort;
    @Mock private AccountApplicationService accountApplicationService;

    @InjectMocks private TransferApplicationService service;

    private static Account account(Long id, String name) {
        return new Account(
                id, 10L, name, AccountType.CHECKING, BigDecimal.ZERO, LocalDateTime.now());
    }

    @Test
    void createThrowsWhenFromAndToAccountsAreTheSame() {
        assertThatThrownBy(() -> service.create(10L, 1L, 1L, BigDecimal.TEN, LocalDate.now(), null))
                .isInstanceOf(SameAccountTransferException.class);
    }

    @Test
    void createThrowsWhenAmountExceedsSourceAccountBalance() {
        Account from = account(1L, "Origem");
        Account to = account(2L, "Destino");
        when(accountRepositoryPort.findById(1L)).thenReturn(Optional.of(from));
        when(accountRepositoryPort.findById(2L)).thenReturn(Optional.of(to));
        when(accountApplicationService.calculateCurrentBalance(from))
                .thenReturn(BigDecimal.valueOf(50));

        assertThatThrownBy(
                        () ->
                                service.create(
                                        10L,
                                        1L,
                                        2L,
                                        BigDecimal.valueOf(100),
                                        LocalDate.now(),
                                        null))
                .isInstanceOf(InsufficientBalanceException.class);
    }

    @Test
    void createSavesTransferAndBothTransactionLegsWithDefaultDescriptions() {
        Account from = account(1L, "Origem");
        Account to = account(2L, "Destino");
        when(accountRepositoryPort.findById(1L)).thenReturn(Optional.of(from));
        when(accountRepositoryPort.findById(2L)).thenReturn(Optional.of(to));
        when(accountApplicationService.calculateCurrentBalance(from))
                .thenReturn(BigDecimal.valueOf(500));
        when(transferRepositoryPort.save(any()))
                .thenAnswer(
                        invocation -> {
                            Transfer transfer = invocation.getArgument(0);
                            return new Transfer(
                                    99L,
                                    transfer.userId(),
                                    transfer.fromAccountId(),
                                    transfer.toAccountId(),
                                    transfer.amount(),
                                    transfer.transferDate(),
                                    transfer.description(),
                                    transfer.createdAt());
                        });
        when(transactionRepositoryPort.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TransferResult result =
                service.create(10L, 1L, 2L, BigDecimal.valueOf(200), LocalDate.now(), null);

        assertThat(result.transfer().id()).isEqualTo(99L);
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepositoryPort, org.mockito.Mockito.times(2)).save(captor.capture());
        assertThat(captor.getAllValues())
                .anySatisfy(
                        t -> {
                            assertThat(t.type()).isEqualTo(CategoryType.EXPENSE);
                            assertThat(t.accountId()).isEqualTo(1L);
                            assertThat(t.description()).isEqualTo("Transferência para Destino");
                        })
                .anySatisfy(
                        t -> {
                            assertThat(t.type()).isEqualTo(CategoryType.INCOME);
                            assertThat(t.accountId()).isEqualTo(2L);
                            assertThat(t.description()).isEqualTo("Transferência de Origem");
                        });
    }

    @Test
    void createUsesDefaultDescriptionWhenProvidedDescriptionIsBlank() {
        Account from = account(1L, "Origem");
        Account to = account(2L, "Destino");
        when(accountRepositoryPort.findById(1L)).thenReturn(Optional.of(from));
        when(accountRepositoryPort.findById(2L)).thenReturn(Optional.of(to));
        when(accountApplicationService.calculateCurrentBalance(from))
                .thenReturn(BigDecimal.valueOf(500));
        when(transferRepositoryPort.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepositoryPort.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.create(10L, 1L, 2L, BigDecimal.valueOf(200), LocalDate.now(), "   ");

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepositoryPort, org.mockito.Mockito.times(2)).save(captor.capture());
        assertThat(captor.getAllValues())
                .anySatisfy(
                        t -> assertThat(t.description()).isEqualTo("Transferência para Destino"))
                .anySatisfy(t -> assertThat(t.description()).isEqualTo("Transferência de Origem"));
    }

    @Test
    void createUsesCustomDescriptionForBothLegsWhenProvided() {
        Account from = account(1L, "Origem");
        Account to = account(2L, "Destino");
        when(accountRepositoryPort.findById(1L)).thenReturn(Optional.of(from));
        when(accountRepositoryPort.findById(2L)).thenReturn(Optional.of(to));
        when(accountApplicationService.calculateCurrentBalance(from))
                .thenReturn(BigDecimal.valueOf(500));
        when(transferRepositoryPort.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepositoryPort.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.create(
                10L, 1L, 2L, BigDecimal.valueOf(200), LocalDate.now(), "Reserva de emergência");

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepositoryPort, org.mockito.Mockito.times(2)).save(captor.capture());
        assertThat(captor.getAllValues())
                .allSatisfy(t -> assertThat(t.description()).isEqualTo("Reserva de emergência"));
    }

    @Test
    void deleteThrowsWhenTransferBelongsToAnotherUser() {
        Transfer transfer =
                new Transfer(
                        1L,
                        10L,
                        1L,
                        2L,
                        BigDecimal.TEN,
                        LocalDate.now(),
                        null,
                        LocalDateTime.now());
        when(transferRepositoryPort.findById(1L)).thenReturn(Optional.of(transfer));

        assertThatThrownBy(() -> service.delete(999L, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteRemovesTransferWhenOwned() {
        Transfer transfer =
                new Transfer(
                        1L,
                        10L,
                        1L,
                        2L,
                        BigDecimal.TEN,
                        LocalDate.now(),
                        null,
                        LocalDateTime.now());
        when(transferRepositoryPort.findById(1L)).thenReturn(Optional.of(transfer));

        service.delete(10L, 1L);

        verify(transferRepositoryPort).deleteById(1L);
    }
}
