package com.lmf.finpro.application.debt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lmf.finpro.domain.exception.ResourceNotFoundException;
import com.lmf.finpro.domain.model.Debt;
import com.lmf.finpro.domain.model.DebtBalance;
import com.lmf.finpro.domain.model.DebtType;
import com.lmf.finpro.domain.port.out.DebtBalanceRepositoryPort;
import com.lmf.finpro.domain.port.out.DebtRepositoryPort;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DebtApplicationServiceTest {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 27);
    private static final Long USER_ID = 10L;
    private static final Debt CAR =
            new Debt(5L, USER_ID, "Carro", DebtType.FINANCING, "Banco", null);

    @Mock private DebtRepositoryPort debtRepositoryPort;
    @Mock private DebtBalanceRepositoryPort debtBalanceRepositoryPort;

    private DebtApplicationService service;

    @BeforeEach
    void setUp() {
        service =
                new DebtApplicationService(
                        debtRepositoryPort,
                        debtBalanceRepositoryPort,
                        Clock.fixed(TODAY.atTime(10, 0).atZone(ZONE).toInstant(), ZONE));
    }

    @Test
    void createsTheDebtWithItsFirstBalanceAndBlankCreditorBecomesNull() {
        when(debtRepositoryPort.save(any()))
                .thenAnswer(
                        invocation -> {
                            Debt debt = invocation.getArgument(0);
                            return new Debt(
                                    5L,
                                    debt.userId(),
                                    debt.name(),
                                    debt.type(),
                                    debt.creditor(),
                                    debt.createdAt());
                        });
        when(debtBalanceRepositoryPort.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DebtApplicationService.DebtSummary summary =
                service.create(
                        USER_ID,
                        " Carro ",
                        DebtType.FINANCING,
                        "  ",
                        new BigDecimal("30000"),
                        TODAY);

        assertThat(summary.debt().name()).isEqualTo("Carro");
        assertThat(summary.debt().creditor()).isNull();
        assertThat(summary.lastBalance().debtId()).isEqualTo(5L);
        assertThat(summary.currentBalance()).isEqualByComparingTo("30000");
    }

    @Test
    void listShowsTheLatestBalanceOfEachDebt() {
        when(debtRepositoryPort.findAllByUserId(USER_ID)).thenReturn(List.of(CAR));
        when(debtBalanceRepositoryPort.findAllByDebtIds(List.of(5L)))
                .thenReturn(
                        List.of(
                                balance(1L, "2026-07-01", "30000"),
                                balance(2L, "2026-09-01", "28000")));

        DebtApplicationService.DebtSummary summary = service.list(USER_ID).get(0);

        assertThat(summary.currentBalance()).isEqualByComparingTo("28000");
        assertThat(summary.lastBalance().balanceDate()).isEqualTo(LocalDate.of(2026, 9, 1));
    }

    @Test
    void rejectsFutureDatesAndDeletingTheOnlyBalance() {
        when(debtRepositoryPort.findById(5L)).thenReturn(Optional.of(CAR));
        assertThatThrownBy(
                        () -> service.saveBalance(USER_ID, 5L, TODAY.plusDays(1), BigDecimal.ONE))
                .isInstanceOf(IllegalArgumentException.class);

        when(debtBalanceRepositoryPort.findById(1L))
                .thenReturn(Optional.of(balance(1L, "2026-07-01", "30000")));
        when(debtBalanceRepositoryPort.countByDebtId(5L)).thenReturn(1L);
        assertThatThrownBy(() -> service.deleteBalance(USER_ID, 5L, 1L))
                .isInstanceOf(IllegalArgumentException.class);
        verify(debtBalanceRepositoryPort, never()).deleteById(any());
    }

    @Test
    void debtOfAnotherUserIsNotFound() {
        when(debtRepositoryPort.findById(5L))
                .thenReturn(Optional.of(new Debt(5L, 99L, "Carro", DebtType.LOAN, null, null)));

        assertThatThrownBy(() -> service.delete(USER_ID, 5L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(debtRepositoryPort, never()).deleteById(any());
    }

    private static DebtBalance balance(Long id, String date, String value) {
        return new DebtBalance(id, 5L, LocalDate.parse(date), new BigDecimal(value), null);
    }
}
