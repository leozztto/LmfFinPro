package com.lmf.finpro.application.insight;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.lmf.finpro.domain.model.Account;
import com.lmf.finpro.domain.model.AccountType;
import com.lmf.finpro.domain.model.AlertType;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.Insight;
import com.lmf.finpro.domain.model.Transaction;
import com.lmf.finpro.domain.model.TransactionOrigin;
import com.lmf.finpro.domain.model.TransactionStatus;
import com.lmf.finpro.domain.port.out.AccountRepositoryPort;
import com.lmf.finpro.domain.port.out.CategoryRepositoryPort;
import com.lmf.finpro.domain.port.out.ClientRepositoryPort;
import com.lmf.finpro.domain.port.out.TransactionRepositoryPort;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InsightApplicationServiceTest {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 18);
    private static final Long HOUSEHOLD_ID = 20L;

    @Mock private AccountRepositoryPort accountRepositoryPort;
    @Mock private TransactionRepositoryPort transactionRepositoryPort;
    @Mock private ClientRepositoryPort clientRepositoryPort;
    @Mock private CategoryRepositoryPort categoryRepositoryPort;

    private InsightApplicationService service() {
        return new InsightApplicationService(
                accountRepositoryPort,
                transactionRepositoryPort,
                clientRepositoryPort,
                categoryRepositoryPort,
                Clock.fixed(TODAY.atTime(8, 0).atZone(ZONE).toInstant(), ZONE));
    }

    @Test
    void withoutAccountsReturnsNothingAndSkipsTransactions() {
        when(accountRepositoryPort.findAllByHouseholdId(HOUSEHOLD_ID)).thenReturn(List.of());

        assertThat(service().list(HOUSEHOLD_ID)).isEmpty();
        verifyNoInteractions(transactionRepositoryPort);
    }

    @Test
    void returnsLateClientInsightFromTheHouseholdTransactions() {
        when(accountRepositoryPort.findAllByHouseholdId(HOUSEHOLD_ID))
                .thenReturn(
                        List.of(
                                new Account(
                                        1L,
                                        HOUSEHOLD_ID,
                                        "Conta",
                                        AccountType.CHECKING,
                                        BigDecimal.ZERO,
                                        null)));
        when(transactionRepositoryPort.findAllByAccountIds(List.of(1L)))
                .thenReturn(
                        List.of(
                                pendingIncome(1L, TODAY.minusDays(40)),
                                pendingIncome(2L, TODAY.minusDays(10))));
        when(clientRepositoryPort.findAllByHouseholdId(HOUSEHOLD_ID)).thenReturn(List.of());

        List<Insight> insights = service().list(HOUSEHOLD_ID);

        assertThat(insights).hasSize(1);
        assertThat(insights.get(0).type()).isEqualTo(AlertType.INSIGHT_LATE_CLIENT);
        assertThat(insights.get(0).count()).isEqualTo(2);
    }

    private static Transaction pendingIncome(Long id, LocalDate date) {
        return new Transaction(
                id,
                1L,
                null,
                7L,
                "Serviço",
                BigDecimal.valueOf(500),
                date,
                CategoryType.INCOME,
                TransactionOrigin.MANUAL,
                LocalDateTime.now(),
                null,
                null,
                null,
                TransactionStatus.PENDING);
    }
}
