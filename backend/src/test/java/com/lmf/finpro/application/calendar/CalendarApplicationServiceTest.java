package com.lmf.finpro.application.calendar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.lmf.finpro.application.exchangerate.ExchangeRateApplicationService;
import com.lmf.finpro.domain.model.Account;
import com.lmf.finpro.domain.model.AccountType;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.DocumentType;
import com.lmf.finpro.domain.model.FinancialCalendar.Entry;
import com.lmf.finpro.domain.model.FinancialCalendar.EntryKind;
import com.lmf.finpro.domain.model.TaxEstimate;
import com.lmf.finpro.domain.model.TaxRegime;
import com.lmf.finpro.domain.model.Transaction;
import com.lmf.finpro.domain.model.TransactionOrigin;
import com.lmf.finpro.domain.model.TransactionStatus;
import com.lmf.finpro.domain.model.User;
import com.lmf.finpro.domain.port.out.AccountRepositoryPort;
import com.lmf.finpro.domain.port.out.CategoryRepositoryPort;
import com.lmf.finpro.domain.port.out.ClientRepositoryPort;
import com.lmf.finpro.domain.port.out.RecurringTransactionRepositoryPort;
import com.lmf.finpro.domain.port.out.TaxEstimateRepositoryPort;
import com.lmf.finpro.domain.port.out.TransactionRepositoryPort;
import com.lmf.finpro.domain.port.out.UserRepositoryPort;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CalendarApplicationServiceTest {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 15);
    private static final Long USER_ID = 10L;

    @Mock private ExchangeRateApplicationService exchangeRateApplicationService;

    @Mock private UserRepositoryPort userRepositoryPort;
    @Mock private AccountRepositoryPort accountRepositoryPort;
    @Mock private TransactionRepositoryPort transactionRepositoryPort;
    @Mock private RecurringTransactionRepositoryPort recurringTransactionRepositoryPort;
    @Mock private TaxEstimateRepositoryPort taxEstimateRepositoryPort;
    @Mock private CategoryRepositoryPort categoryRepositoryPort;
    @Mock private ClientRepositoryPort clientRepositoryPort;

    private CalendarApplicationService service;

    @BeforeEach
    void setUp() {
        service =
                new CalendarApplicationService(
                        userRepositoryPort,
                        accountRepositoryPort,
                        transactionRepositoryPort,
                        recurringTransactionRepositoryPort,
                        taxEstimateRepositoryPort,
                        categoryRepositoryPort,
                        clientRepositoryPort,
                        Clock.fixed(TODAY.atTime(10, 0).atZone(ZONE).toInstant(), ZONE),
                        exchangeRateApplicationService);
        lenient()
                .when(accountRepositoryPort.findAllByUserId(USER_ID))
                .thenReturn(
                        List.of(
                                new Account(
                                        1L,
                                        USER_ID,
                                        "Conta PJ",
                                        AccountType.CHECKING,
                                        BigDecimal.ZERO,
                                        null)));
        lenient()
                .when(recurringTransactionRepositoryPort.findAllByUserId(USER_ID))
                .thenReturn(List.of());
        lenient().when(categoryRepositoryPort.findAllVisibleToUser(USER_ID)).thenReturn(List.of());
        lenient().when(clientRepositoryPort.findAllByUserId(USER_ID)).thenReturn(List.of());
    }

    @Test
    void usesCurrentMonthByDefaultLeavesTransfersOutAndResolvesAccountNames() {
        givenUser(TaxRegime.AUTONOMO);
        when(transactionRepositoryPort.findAllByAccountIds(List.of(1L)))
                .thenReturn(
                        List.of(
                                pending(1L, "Boleto", LocalDate.of(2026, 9, 20), null),
                                pending(2L, "Transferência", LocalDate.of(2026, 9, 21), 99L)));

        CalendarApplicationService.Result result = service.build(USER_ID, null, false);

        assertThat(result.report().month()).isEqualTo(YearMonth.of(2026, 9));
        assertThat(result.report().days()).hasSize(1);
        assertThat(result.report().days().get(0).entries().get(0).description())
                .isEqualTo("Boleto");
        assertThat(result.accountNames()).containsEntry(1L, "Conta PJ");
    }

    @Test
    void addsDasWithTheEstimateOfThePreviousCompetence() {
        givenUser(TaxRegime.MEI);
        when(transactionRepositoryPort.findAllByAccountIds(List.of(1L))).thenReturn(List.of());
        when(taxEstimateRepositoryPort.findAllByUserId(USER_ID))
                .thenReturn(
                        List.of(
                                new TaxEstimate(
                                        3L,
                                        USER_ID,
                                        YearMonth.of(2026, 9),
                                        TaxRegime.MEI,
                                        BigDecimal.TEN,
                                        BigDecimal.ONE,
                                        new BigDecimal("80.90"))));

        CalendarApplicationService.Result result =
                service.build(USER_ID, YearMonth.of(2026, 10), false);

        Entry das = result.report().days().get(0).entries().get(0);
        assertThat(das.kind()).isEqualTo(EntryKind.DAS);
        assertThat(das.date()).isEqualTo(LocalDate.of(2026, 10, 20));
        assertThat(das.competence()).isEqualTo(YearMonth.of(2026, 9));
        assertThat(das.amount()).isEqualByComparingTo("80.90");
    }

    @Test
    void userWithoutAccountsGetsAnEmptyCalendar() {
        givenUser(TaxRegime.AUTONOMO);
        when(accountRepositoryPort.findAllByUserId(USER_ID)).thenReturn(List.of());

        CalendarApplicationService.Result result = service.build(USER_ID, null, true);

        assertThat(result.report().days()).isEmpty();
        assertThat(result.report().overdue()).isEmpty();
    }

    @Test
    void rejectsMonthsTooFarAway() {
        assertThatThrownBy(() -> service.build(USER_ID, YearMonth.of(2032, 1), false))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.build(USER_ID, YearMonth.of(2021, 8), false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private void givenUser(TaxRegime regime) {
        when(userRepositoryPort.findById(USER_ID))
                .thenReturn(
                        Optional.of(
                                new User(
                                        USER_ID,
                                        "Pessoa",
                                        "pessoa@teste.com",
                                        "hash",
                                        DocumentType.CPF,
                                        "00000000000",
                                        null,
                                        regime,
                                        null,
                                        null,
                                        0)));
    }

    private static Transaction pending(
            Long id, String description, LocalDate date, Long transferId) {
        return new Transaction(
                id,
                1L,
                null,
                null,
                description,
                new BigDecimal("100"),
                date,
                CategoryType.EXPENSE,
                TransactionOrigin.MANUAL,
                null,
                transferId,
                null,
                null,
                TransactionStatus.PENDING);
    }
}
