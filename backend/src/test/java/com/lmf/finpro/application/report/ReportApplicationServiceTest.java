package com.lmf.finpro.application.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lmf.finpro.domain.exception.ResourceNotFoundException;
import com.lmf.finpro.domain.model.Address;
import com.lmf.finpro.domain.model.BrazilianState;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.Client;
import com.lmf.finpro.domain.model.ClientReceiptData;
import com.lmf.finpro.domain.model.ClientWorkType;
import com.lmf.finpro.domain.model.DocumentType;
import com.lmf.finpro.domain.model.TaxRegime;
import com.lmf.finpro.domain.model.Transaction;
import com.lmf.finpro.domain.model.TransactionOrigin;
import com.lmf.finpro.domain.model.User;
import com.lmf.finpro.domain.port.out.ClientRepositoryPort;
import com.lmf.finpro.domain.port.out.ReceiptGeneratorPort;
import com.lmf.finpro.domain.port.out.TransactionRepositoryPort;
import com.lmf.finpro.domain.port.out.UserRepositoryPort;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReportApplicationServiceTest {

    @Mock private ClientRepositoryPort clientRepositoryPort;
    @Mock private UserRepositoryPort userRepositoryPort;
    @Mock private TransactionRepositoryPort transactionRepositoryPort;
    @Mock private ReceiptGeneratorPort receiptGeneratorPort;

    @InjectMocks private ReportApplicationService service;

    private static Client ownedClient() {
        return new Client(
                1L,
                10L,
                "Cliente X",
                "cliente@x.com",
                "11987654321",
                DocumentType.CPF,
                "52998224725",
                ClientWorkType.PJ,
                null,
                null,
                true);
    }

    private static User issuer() {
        Address address =
                new Address(
                        "01310100",
                        "Av. Paulista",
                        "1000",
                        null,
                        "Bela Vista",
                        "São Paulo",
                        BrazilianState.SP);
        return new User(
                10L,
                "Prestador",
                "prestador@x.com",
                "hash",
                DocumentType.CPF,
                "11144477735",
                "11999998888",
                TaxRegime.AUTONOMO,
                address,
                LocalDateTime.now());
    }

    @Test
    void throwsWhenClientDoesNotBelongToCurrentUser() {
        when(clientRepositoryPort.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generateClientReceipt(10L, 1L, YearMonth.of(2026, 9)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void throwsWhenClientBelongsToAnotherUser() {
        Client otherUsersClient =
                new Client(
                        1L,
                        999L,
                        "Outro",
                        null,
                        null,
                        DocumentType.CPF,
                        "52998224725",
                        ClientWorkType.PJ,
                        null,
                        null,
                        true);
        when(clientRepositoryPort.findById(1L)).thenReturn(Optional.of(otherUsersClient));

        assertThatThrownBy(() -> service.generateClientReceipt(10L, 1L, YearMonth.of(2026, 9)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void sumsTransactionsAndPassesEverythingToTheGenerator() {
        User issuer = issuer();
        when(clientRepositoryPort.findById(1L)).thenReturn(Optional.of(ownedClient()));
        when(userRepositoryPort.findById(10L)).thenReturn(Optional.of(issuer));
        Transaction t1 =
                new Transaction(
                        1L,
                        5L,
                        null,
                        1L,
                        "Serviço A",
                        BigDecimal.valueOf(1000),
                        LocalDate.of(2026, 9, 5),
                        CategoryType.INCOME,
                        TransactionOrigin.MANUAL,
                        LocalDateTime.now(),
                        null,
                        null);
        Transaction t2 =
                new Transaction(
                        2L,
                        5L,
                        null,
                        1L,
                        "Serviço B",
                        BigDecimal.valueOf(500),
                        LocalDate.of(2026, 9, 20),
                        CategoryType.INCOME,
                        TransactionOrigin.MANUAL,
                        LocalDateTime.now(),
                        null,
                        null);
        when(transactionRepositoryPort.findAllByClientIdAndTypeAndDateBetween(
                        eq(1L),
                        eq(CategoryType.INCOME),
                        eq(LocalDate.of(2026, 9, 1)),
                        eq(LocalDate.of(2026, 10, 1))))
                .thenReturn(List.of(t1, t2));
        when(receiptGeneratorPort.generateClientReceipt(any())).thenReturn(new byte[] {1, 2, 3});

        byte[] result = service.generateClientReceipt(10L, 1L, YearMonth.of(2026, 9));

        assertThat(result).containsExactly(1, 2, 3);
        ArgumentCaptor<ClientReceiptData> captor = ArgumentCaptor.forClass(ClientReceiptData.class);
        verify(receiptGeneratorPort).generateClientReceipt(captor.capture());
        ClientReceiptData data = captor.getValue();
        assertThat(data.total()).isEqualByComparingTo("1500");
        assertThat(data.transactions()).containsExactly(t1, t2);
        assertThat(data.client()).isEqualTo(ownedClient());
        assertThat(data.issuer()).isEqualTo(issuer);
        assertThat(data.referenceMonth()).isEqualTo(YearMonth.of(2026, 9));
    }

    @Test
    void totalIsZeroWhenThereAreNoTransactionsInThePeriod() {
        when(clientRepositoryPort.findById(1L)).thenReturn(Optional.of(ownedClient()));
        when(userRepositoryPort.findById(10L)).thenReturn(Optional.of(issuer()));
        when(transactionRepositoryPort.findAllByClientIdAndTypeAndDateBetween(
                        any(), any(), any(), any()))
                .thenReturn(List.of());
        when(receiptGeneratorPort.generateClientReceipt(any())).thenReturn(new byte[0]);

        service.generateClientReceipt(10L, 1L, YearMonth.of(2026, 9));

        ArgumentCaptor<ClientReceiptData> captor = ArgumentCaptor.forClass(ClientReceiptData.class);
        verify(receiptGeneratorPort).generateClientReceipt(captor.capture());
        assertThat(captor.getValue().total()).isEqualByComparingTo("0");
    }
}
