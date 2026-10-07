package com.lmf.finpro.application.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.lmf.finpro.domain.model.Account;
import com.lmf.finpro.domain.model.AccountType;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.Client;
import com.lmf.finpro.domain.model.ClientWorkType;
import com.lmf.finpro.domain.model.Transaction;
import com.lmf.finpro.domain.model.TransactionOrigin;
import com.lmf.finpro.domain.model.TransactionStatus;
import com.lmf.finpro.domain.port.out.AccountRepositoryPort;
import com.lmf.finpro.domain.port.out.ClientRepositoryPort;
import com.lmf.finpro.domain.port.out.TransactionRepositoryPort;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClientAnalyticsApplicationServiceTest {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 26);
    private static final Long USER_ID = 10L;

    @Mock private AccountRepositoryPort accountRepositoryPort;
    @Mock private TransactionRepositoryPort transactionRepositoryPort;
    @Mock private ClientRepositoryPort clientRepositoryPort;

    private ClientAnalyticsApplicationService service;

    @BeforeEach
    void setUp() {
        service =
                new ClientAnalyticsApplicationService(
                        accountRepositoryPort,
                        transactionRepositoryPort,
                        clientRepositoryPort,
                        Clock.fixed(TODAY.atTime(10, 0).atZone(ZONE).toInstant(), ZONE));
        lenient()
                .when(accountRepositoryPort.findAllByHouseholdId(USER_ID))
                .thenReturn(
                        List.of(
                                new Account(
                                        1L,
                                        USER_ID,
                                        "Conta",
                                        AccountType.CHECKING,
                                        BigDecimal.ZERO,
                                        null)));
        lenient()
                .when(clientRepositoryPort.findAllByHouseholdId(USER_ID))
                .thenReturn(
                        List.of(
                                new Client(
                                        5L,
                                        USER_ID,
                                        "Cliente X",
                                        null,
                                        null,
                                        null,
                                        null,
                                        ClientWorkType.PJ,
                                        null,
                                        "#123456",
                                        true)));
        lenient().when(transactionRepositoryPort.findAllByAccountIds(any())).thenReturn(List.of());
    }

    @Test
    void periodEndsInTheCurrentMonth() {
        ClientAnalyticsApplicationService.Result result = service.analyze(USER_ID, 3, false);

        assertThat(result.report().months())
                .containsExactly(
                        YearMonth.of(2026, 7), YearMonth.of(2026, 8), YearMonth.of(2026, 9));
        assertThat(result.clientsById().get(5L).name()).isEqualTo("Cliente X");
    }

    @Test
    void ignoresTransfersAndCanKeepOnlyReceivedIncome() {
        when(transactionRepositoryPort.findAllByAccountIds(List.of(1L)))
                .thenReturn(
                        List.of(
                                income("1000", TransactionStatus.PAID, null),
                                income("500", TransactionStatus.PENDING, null),
                                income("9999", TransactionStatus.PAID, 77L)));

        assertThat(service.analyze(USER_ID, 3, false).report().totalIncome())
                .isEqualByComparingTo("1500");
        assertThat(service.analyze(USER_ID, 3, true).report().totalIncome())
                .isEqualByComparingTo("1000");
    }

    @Test
    void rejectsPeriodOutOfRange() {
        assertThatThrownBy(() -> service.analyze(USER_ID, 0, false))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.analyze(USER_ID, 37, false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static Transaction income(String amount, TransactionStatus status, Long transferId) {
        return new Transaction(
                null,
                1L,
                null,
                5L,
                "Receita",
                new BigDecimal(amount),
                TODAY,
                CategoryType.INCOME,
                TransactionOrigin.MANUAL,
                null,
                transferId,
                null,
                null,
                status);
    }
}
