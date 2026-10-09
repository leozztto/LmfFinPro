package com.lmf.finpro.application.alert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lmf.finpro.application.budget.BudgetApplicationService;
import com.lmf.finpro.application.push.PushNotificationApplicationService;
import com.lmf.finpro.domain.model.Account;
import com.lmf.finpro.domain.model.AccountType;
import com.lmf.finpro.domain.model.AlertDigest;
import com.lmf.finpro.domain.model.AlertType;
import com.lmf.finpro.domain.model.Budget;
import com.lmf.finpro.domain.model.Category;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.HouseholdMembership;
import com.lmf.finpro.domain.model.HouseholdRole;
import com.lmf.finpro.domain.model.NotificationPreferences;
import com.lmf.finpro.domain.model.RecurringBudget;
import com.lmf.finpro.domain.model.SentAlert;
import com.lmf.finpro.domain.model.TaxEstimate;
import com.lmf.finpro.domain.model.TaxRegime;
import com.lmf.finpro.domain.model.Transaction;
import com.lmf.finpro.domain.model.TransactionOrigin;
import com.lmf.finpro.domain.model.TransactionStatus;
import com.lmf.finpro.domain.model.User;
import com.lmf.finpro.domain.port.out.AccountRepositoryPort;
import com.lmf.finpro.domain.port.out.AlertMailerPort;
import com.lmf.finpro.domain.port.out.BudgetRepositoryPort;
import com.lmf.finpro.domain.port.out.CategoryRepositoryPort;
import com.lmf.finpro.domain.port.out.ClientRepositoryPort;
import com.lmf.finpro.domain.port.out.HouseholdRepositoryPort;
import com.lmf.finpro.domain.port.out.NotificationPreferencesRepositoryPort;
import com.lmf.finpro.domain.port.out.RecurringBudgetRepositoryPort;
import com.lmf.finpro.domain.port.out.SentAlertRepositoryPort;
import com.lmf.finpro.domain.port.out.TaxEstimateRepositoryPort;
import com.lmf.finpro.domain.port.out.TransactionRepositoryPort;
import com.lmf.finpro.domain.port.out.UserRepositoryPort;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AlertApplicationServiceTest {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    // Sexta, 18/09/2026: o DAS de 20/09 (competência 08/2026) está dentro da janela padrão de 3
    // dias.
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 18);
    private static final Long USER_ID = 10L;
    // Distinto de USER_ID de propósito: os dados são buscados pelo grupo, os envios são do usuário.
    private static final Long HOUSEHOLD_ID = 20L;

    @Mock private UserRepositoryPort userRepositoryPort;
    @Mock private HouseholdRepositoryPort householdRepositoryPort;
    @Mock private NotificationPreferencesRepositoryPort notificationPreferencesRepositoryPort;
    @Mock private SentAlertRepositoryPort sentAlertRepositoryPort;
    @Mock private AccountRepositoryPort accountRepositoryPort;
    @Mock private TransactionRepositoryPort transactionRepositoryPort;
    @Mock private BudgetRepositoryPort budgetRepositoryPort;
    @Mock private BudgetApplicationService budgetApplicationService;
    @Mock private RecurringBudgetRepositoryPort recurringBudgetRepositoryPort;
    @Mock private CategoryRepositoryPort categoryRepositoryPort;
    @Mock private ClientRepositoryPort clientRepositoryPort;
    @Mock private TaxEstimateRepositoryPort taxEstimateRepositoryPort;
    @Mock private AlertMailerPort alertMailerPort;
    @Mock private PushNotificationApplicationService pushNotificationApplicationService;

    @BeforeEach
    void setUp() {
        HouseholdMembership personal =
                new HouseholdMembership(HOUSEHOLD_ID, USER_ID, HouseholdRole.OWNER);
        lenient()
                .when(householdRepositoryPort.findMembershipsByUserId(USER_ID))
                .thenReturn(List.of(personal));
        lenient()
                .when(householdRepositoryPort.findPersonalMembership(USER_ID))
                .thenReturn(Optional.of(personal));
        lenient()
                .when(notificationPreferencesRepositoryPort.findByUserId(USER_ID))
                .thenReturn(Optional.empty());
        lenient()
                .when(accountRepositoryPort.findAllByHouseholdId(HOUSEHOLD_ID))
                .thenReturn(
                        List.of(
                                new Account(
                                        1L,
                                        HOUSEHOLD_ID,
                                        "Conta",
                                        AccountType.CHECKING,
                                        BigDecimal.ZERO,
                                        null)));
        lenient().when(transactionRepositoryPort.findAllByAccountIds(any())).thenReturn(List.of());
        lenient()
                .when(budgetRepositoryPort.findAllByHouseholdId(HOUSEHOLD_ID))
                .thenReturn(List.of());
        lenient()
                .when(recurringBudgetRepositoryPort.findAllByHouseholdId(HOUSEHOLD_ID))
                .thenReturn(List.of());
        lenient().when(sentAlertRepositoryPort.exists(anyLong(), any(), any())).thenReturn(false);
        lenient()
                .when(taxEstimateRepositoryPort.findAllByHouseholdId(HOUSEHOLD_ID))
                .thenReturn(List.of());
        lenient()
                .when(categoryRepositoryPort.findById(5L))
                .thenReturn(
                        Optional.of(
                                new Category(
                                        5L,
                                        HOUSEHOLD_ID,
                                        "Mercado",
                                        CategoryType.EXPENSE,
                                        null,
                                        null)));
    }

    @Test
    void sendsOnlyPendingExpensesDueWithinTheWindowAndRecordsThem() {
        when(transactionRepositoryPort.findAllByAccountIds(List.of(1L)))
                .thenReturn(
                        List.of(
                                expense(1L, TODAY.plusDays(1), TransactionStatus.PENDING),
                                expense(2L, TODAY.plusDays(5), TransactionStatus.PENDING),
                                expense(3L, TODAY.plusDays(2), TransactionStatus.PAID),
                                expense(4L, TODAY.minusDays(1), TransactionStatus.PENDING)));

        boolean sent = service(TODAY).sendAlertsTo(user(TaxRegime.AUTONOMO));

        assertThat(sent).isTrue();
        AlertDigest digest = sentDigest();
        assertThat(digest.bills()).hasSize(1);
        assertThat(digest.bills().get(0).dueDate()).isEqualTo(TODAY.plusDays(1));
        verify(sentAlertRepositoryPort).save(new SentAlert(USER_ID, AlertType.BILL_DUE, "1"));
    }

    @Test
    void sendAlertsToUnknownUserIdSendsNothing() {
        when(userRepositoryPort.findById(99L)).thenReturn(Optional.empty());

        assertThat(service(TODAY).sendAlertsTo(99L)).isFalse();
        verify(alertMailerPort, never()).sendDigest(any(), any(), any());
    }

    @Test
    void doesNotRepeatABillAlreadyNotified() {
        when(transactionRepositoryPort.findAllByAccountIds(List.of(1L)))
                .thenReturn(List.of(expense(1L, TODAY.plusDays(1), TransactionStatus.PENDING)));
        when(sentAlertRepositoryPort.exists(USER_ID, AlertType.BILL_DUE, "1")).thenReturn(true);

        boolean sent = service(TODAY).sendAlertsTo(user(TaxRegime.AUTONOMO));

        assertThat(sent).isFalse();
        verify(alertMailerPort, never()).sendDigest(any(), any(), any());
    }

    @Test
    void sendsPendingExpensesPastDueWithinTheLimitAndRecordsThemOnce() {
        when(transactionRepositoryPort.findAllByAccountIds(List.of(1L)))
                .thenReturn(
                        List.of(
                                expense(1L, TODAY.minusDays(2), TransactionStatus.PENDING),
                                expense(2L, TODAY.minusDays(10), TransactionStatus.PENDING),
                                // paga, hoje, futura e antiga demais não são "atrasadas" a avisar
                                expense(3L, TODAY.minusDays(3), TransactionStatus.PAID),
                                expense(4L, TODAY, TransactionStatus.PENDING),
                                expense(5L, TODAY.plusDays(1), TransactionStatus.PENDING),
                                expense(
                                        6L,
                                        TODAY.minusDays(
                                                AlertApplicationService.MAX_OVERDUE_DAYS + 1),
                                        TransactionStatus.PENDING)));

        boolean sent = service(TODAY).sendAlertsTo(user(TaxRegime.AUTONOMO));

        assertThat(sent).isTrue();
        AlertDigest digest = sentDigest();
        assertThat(digest.overdueBills()).hasSize(2);
        // a mais atrasada primeiro
        assertThat(digest.overdueBills().get(0).dueDate()).isEqualTo(TODAY.minusDays(10));
        assertThat(digest.overdueBills().get(0).daysOverdue()).isEqualTo(10);
        assertThat(digest.overdueBills().get(1).daysOverdue()).isEqualTo(2);
        verify(sentAlertRepositoryPort).save(new SentAlert(USER_ID, AlertType.BILL_OVERDUE, "1"));
        verify(sentAlertRepositoryPort).save(new SentAlert(USER_ID, AlertType.BILL_OVERDUE, "2"));
        verify(sentAlertRepositoryPort, never())
                .save(new SentAlert(USER_ID, AlertType.BILL_OVERDUE, "6"));
    }

    @Test
    void doesNotRepeatAnOverdueBillAlreadyNotified() {
        when(transactionRepositoryPort.findAllByAccountIds(List.of(1L)))
                .thenReturn(List.of(expense(1L, TODAY.minusDays(2), TransactionStatus.PENDING)));
        when(sentAlertRepositoryPort.exists(USER_ID, AlertType.BILL_OVERDUE, "1")).thenReturn(true);

        assertThat(service(TODAY).sendAlertsTo(user(TaxRegime.AUTONOMO))).isFalse();
        verify(alertMailerPort, never()).sendDigest(any(), any(), any());
    }

    @Test
    void budgetOfASharedHouseholdIsAlertedToo() {
        Long sharedHouseholdId = 30L;
        lenient()
                .when(householdRepositoryPort.findMembershipsByUserId(USER_ID))
                .thenReturn(
                        List.of(
                                new HouseholdMembership(HOUSEHOLD_ID, USER_ID, HouseholdRole.OWNER),
                                new HouseholdMembership(
                                        sharedHouseholdId, USER_ID, HouseholdRole.MEMBER)));
        Budget sharedBudget =
                new Budget(
                        9L,
                        sharedHouseholdId,
                        5L,
                        YearMonth.from(TODAY),
                        BigDecimal.valueOf(1000),
                        null);
        when(budgetRepositoryPort.findAllByHouseholdId(sharedHouseholdId))
                .thenReturn(List.of(sharedBudget));
        when(budgetApplicationService.calculateSpent(sharedBudget))
                .thenReturn(new BigDecimal("1200"));

        service(TODAY).sendAlertsTo(user(TaxRegime.AUTONOMO));

        assertThat(sentDigest().budgets()).hasSize(1);
        verify(sentAlertRepositoryPort).save(new SentAlert(USER_ID, AlertType.BUDGET_100, "9"));
    }

    @Test
    void budgetBelowEightyPercentIsNotAlerted() {
        givenBudgetWithSpent("790");

        assertThat(service(TODAY).sendAlertsTo(user(TaxRegime.AUTONOMO))).isFalse();
    }

    @Test
    void budgetAboveEightyPercentSendsWarning() {
        givenBudgetWithSpent("850");

        service(TODAY).sendAlertsTo(user(TaxRegime.AUTONOMO));

        AlertDigest digest = sentDigest();
        assertThat(digest.budgets()).hasSize(1);
        assertThat(digest.budgets().get(0).threshold()).isEqualTo(80);
        assertThat(digest.budgets().get(0).categoryName()).isEqualTo("Mercado");
        verify(sentAlertRepositoryPort).save(new SentAlert(USER_ID, AlertType.BUDGET_80, "7"));
    }

    @Test
    void budgetOverLimitSendsOnlyTheHundredPercentAlertAndMarksBoth() {
        givenBudgetWithSpent("1200");

        service(TODAY).sendAlertsTo(user(TaxRegime.AUTONOMO));

        AlertDigest digest = sentDigest();
        assertThat(digest.budgets()).singleElement().extracting("threshold").isEqualTo(100);
        verify(sentAlertRepositoryPort).save(new SentAlert(USER_ID, AlertType.BUDGET_100, "7"));
        verify(sentAlertRepositoryPort).save(new SentAlert(USER_ID, AlertType.BUDGET_80, "7"));
    }

    @Test
    void budgetAlreadyWarnedAtEightyIsNotWarnedAgainBelowLimit() {
        givenBudgetWithSpent("900");
        when(sentAlertRepositoryPort.exists(USER_ID, AlertType.BUDGET_80, "7")).thenReturn(true);

        assertThat(service(TODAY).sendAlertsTo(user(TaxRegime.AUTONOMO))).isFalse();
    }

    @Test
    void budgetOfAnotherMonthIsIgnored() {
        when(budgetRepositoryPort.findAllByHouseholdId(HOUSEHOLD_ID))
                .thenReturn(
                        List.of(
                                new Budget(
                                        7L,
                                        HOUSEHOLD_ID,
                                        5L,
                                        YearMonth.from(TODAY).minusMonths(1),
                                        BigDecimal.valueOf(1000),
                                        null)));

        assertThat(service(TODAY).sendAlertsTo(user(TaxRegime.AUTONOMO))).isFalse();
    }

    @Test
    void recurringBudgetEndingThisMonthSendsWarning() {
        givenRecurringBudget(3L, YearMonth.from(TODAY), true);

        service(TODAY).sendAlertsTo(user(TaxRegime.AUTONOMO));

        AlertDigest digest = sentDigest();
        assertThat(digest.recurringBudgetsExpiring()).hasSize(1);
        assertThat(digest.recurringBudgetsExpiring().get(0).categoryName()).isEqualTo("Mercado");
        assertThat(digest.recurringBudgetsExpiring().get(0).endMonth())
                .isEqualTo(YearMonth.from(TODAY));
        verify(sentAlertRepositoryPort)
                .save(new SentAlert(USER_ID, AlertType.RECURRING_BUDGET_EXPIRING, "3-2026-09"));
    }

    @Test
    void recurringBudgetEndingNextMonthSendsWarning() {
        givenRecurringBudget(3L, YearMonth.from(TODAY).plusMonths(1), true);

        assertThat(service(TODAY).sendAlertsTo(user(TaxRegime.AUTONOMO))).isTrue();
        assertThat(sentDigest().recurringBudgetsExpiring()).hasSize(1);
    }

    @Test
    void recurringBudgetEndingFarInTheFutureIsIgnored() {
        givenRecurringBudget(3L, YearMonth.from(TODAY).plusMonths(2), true);

        assertThat(service(TODAY).sendAlertsTo(user(TaxRegime.AUTONOMO))).isFalse();
    }

    @Test
    void inactiveRecurringBudgetIsIgnored() {
        givenRecurringBudget(3L, YearMonth.from(TODAY), false);

        assertThat(service(TODAY).sendAlertsTo(user(TaxRegime.AUTONOMO))).isFalse();
    }

    @Test
    void recurringBudgetWithoutEndMonthIsIgnored() {
        when(recurringBudgetRepositoryPort.findAllByHouseholdId(HOUSEHOLD_ID))
                .thenReturn(
                        List.of(
                                new RecurringBudget(
                                        3L,
                                        HOUSEHOLD_ID,
                                        5L,
                                        BigDecimal.valueOf(500),
                                        YearMonth.from(TODAY).minusMonths(3),
                                        null,
                                        3,
                                        true,
                                        LocalDateTime.now())));

        assertThat(service(TODAY).sendAlertsTo(user(TaxRegime.AUTONOMO))).isFalse();
    }

    @Test
    void doesNotRepeatARecurringBudgetAlreadyNotifiedForTheSameEndMonth() {
        givenRecurringBudget(3L, YearMonth.from(TODAY), true);
        when(sentAlertRepositoryPort.exists(
                        USER_ID, AlertType.RECURRING_BUDGET_EXPIRING, "3-2026-09"))
                .thenReturn(true);

        assertThat(service(TODAY).sendAlertsTo(user(TaxRegime.AUTONOMO))).isFalse();
    }

    @Test
    void dasReminderForMeiIncludesEstimatedValueOfTheCompetence() {
        YearMonth competence = YearMonth.of(2026, 8);
        when(taxEstimateRepositoryPort.findAllByHouseholdId(HOUSEHOLD_ID))
                .thenReturn(
                        List.of(
                                new TaxEstimate(
                                        1L,
                                        HOUSEHOLD_ID,
                                        competence,
                                        TaxRegime.MEI,
                                        BigDecimal.valueOf(5000),
                                        new BigDecimal("0.06"),
                                        BigDecimal.valueOf(300))));

        service(TODAY).sendAlertsTo(user(TaxRegime.MEI));

        AlertDigest.DasReminder das = sentDigest().das();
        assertThat(das.competence()).isEqualTo(competence);
        assertThat(das.dueDate()).isEqualTo(LocalDate.of(2026, 9, 20));
        assertThat(das.estimatedValue()).isEqualByComparingTo("300");
        verify(sentAlertRepositoryPort).save(new SentAlert(USER_ID, AlertType.DAS_DUE, "2026-08"));
    }

    @Test
    void dasReminderWithoutEstimateHasNoValue() {
        service(TODAY).sendAlertsTo(user(TaxRegime.SIMPLES_NACIONAL));

        assertThat(sentDigest().das().estimatedValue()).isNull();
    }

    @Test
    void noDasReminderForOtherRegimesOrOutsideTheWindow() {
        assertThat(service(TODAY).sendAlertsTo(user(TaxRegime.AUTONOMO))).isFalse();
        assertThat(service(LocalDate.of(2026, 9, 5)).sendAlertsTo(user(TaxRegime.MEI))).isFalse();
    }

    @Test
    void disabledPreferencesSkipEveryAlert() {
        when(notificationPreferencesRepositoryPort.findByUserId(USER_ID))
                .thenReturn(
                        Optional.of(
                                new NotificationPreferences(
                                        USER_ID, false, 3, false, false, false, false)));
        lenient()
                .when(transactionRepositoryPort.findAllByAccountIds(List.of(1L)))
                .thenReturn(List.of(expense(1L, TODAY.plusDays(1), TransactionStatus.PENDING)));
        givenBudgetWithSpent("1200");
        givenRecurringBudget(3L, YearMonth.from(TODAY), true);

        assertThat(service(TODAY).sendAlertsTo(user(TaxRegime.MEI))).isFalse();
        verify(alertMailerPort, never()).sendDigest(any(), any(), any());
    }

    @Test
    void sendsLateClientInsightOnceAndRecordsIt() {
        when(transactionRepositoryPort.findAllByAccountIds(List.of(1L)))
                .thenReturn(
                        List.of(
                                pendingIncome(1L, 7L, TODAY.minusDays(40)),
                                pendingIncome(2L, 7L, TODAY.minusDays(10))));

        assertThat(service(TODAY).sendAlertsTo(user(TaxRegime.AUTONOMO))).isTrue();

        AlertDigest digest = sentDigest();
        assertThat(digest.insights()).hasSize(1);
        assertThat(digest.insights().get(0).type()).isEqualTo(AlertType.INSIGHT_LATE_CLIENT);
        verify(sentAlertRepositoryPort)
                .save(new SentAlert(USER_ID, AlertType.INSIGHT_LATE_CLIENT, "7-2026-09"));
    }

    @Test
    void doesNotRepeatInsightAlreadySent() {
        when(transactionRepositoryPort.findAllByAccountIds(List.of(1L)))
                .thenReturn(
                        List.of(
                                pendingIncome(1L, 7L, TODAY.minusDays(40)),
                                pendingIncome(2L, 7L, TODAY.minusDays(10))));
        when(sentAlertRepositoryPort.exists(USER_ID, AlertType.INSIGHT_LATE_CLIENT, "7-2026-09"))
                .thenReturn(true);

        assertThat(service(TODAY).sendAlertsTo(user(TaxRegime.AUTONOMO))).isFalse();
        verify(alertMailerPort, never()).sendDigest(any(), any(), any());
    }

    @Test
    void insightsDisabledSkipsInsights() {
        when(notificationPreferencesRepositoryPort.findByUserId(USER_ID))
                .thenReturn(
                        Optional.of(
                                new NotificationPreferences(
                                        USER_ID, true, 3, true, true, true, false)));
        when(transactionRepositoryPort.findAllByAccountIds(List.of(1L)))
                .thenReturn(
                        List.of(
                                pendingIncome(1L, 7L, TODAY.minusDays(40)),
                                pendingIncome(2L, 7L, TODAY.minusDays(10))));

        assertThat(service(TODAY).sendAlertsTo(user(TaxRegime.AUTONOMO))).isFalse();
    }

    @Test
    void failedSendDoesNotRecordAlerts() {
        when(transactionRepositoryPort.findAllByAccountIds(List.of(1L)))
                .thenReturn(List.of(expense(1L, TODAY.plusDays(1), TransactionStatus.PENDING)));
        doThrow(new RuntimeException("smtp fora"))
                .when(alertMailerPort)
                .sendDigest(any(), any(), any());

        assertThatThrownBy(() -> service(TODAY).sendAlertsTo(user(TaxRegime.AUTONOMO)))
                .isInstanceOf(RuntimeException.class);
        verify(sentAlertRepositoryPort, never()).save(any());
    }

    private AlertApplicationService service(LocalDate today) {
        return new AlertApplicationService(
                userRepositoryPort,
                householdRepositoryPort,
                notificationPreferencesRepositoryPort,
                sentAlertRepositoryPort,
                accountRepositoryPort,
                transactionRepositoryPort,
                budgetRepositoryPort,
                budgetApplicationService,
                recurringBudgetRepositoryPort,
                categoryRepositoryPort,
                clientRepositoryPort,
                taxEstimateRepositoryPort,
                alertMailerPort,
                pushNotificationApplicationService,
                Clock.fixed(today.atTime(8, 0).atZone(ZONE).toInstant(), ZONE));
    }

    private void givenBudgetWithSpent(String spent) {
        Budget budget =
                new Budget(
                        7L,
                        HOUSEHOLD_ID,
                        5L,
                        YearMonth.from(TODAY),
                        BigDecimal.valueOf(1000),
                        null);
        lenient()
                .when(budgetRepositoryPort.findAllByHouseholdId(HOUSEHOLD_ID))
                .thenReturn(List.of(budget));
        lenient()
                .when(budgetApplicationService.calculateSpent(budget))
                .thenReturn(new BigDecimal(spent));
    }

    private void givenRecurringBudget(Long id, YearMonth endMonth, boolean active) {
        RecurringBudget recurrence =
                new RecurringBudget(
                        id,
                        HOUSEHOLD_ID,
                        5L,
                        BigDecimal.valueOf(500),
                        endMonth.minusMonths(6),
                        endMonth,
                        6,
                        active,
                        LocalDateTime.now());
        lenient()
                .when(recurringBudgetRepositoryPort.findAllByHouseholdId(HOUSEHOLD_ID))
                .thenReturn(List.of(recurrence));
    }

    private AlertDigest sentDigest() {
        ArgumentCaptor<AlertDigest> captor = ArgumentCaptor.forClass(AlertDigest.class);
        verify(alertMailerPort).sendDigest(eq("ana@finpro.test"), eq("Ana"), captor.capture());
        return captor.getValue();
    }

    private static User user(TaxRegime regime) {
        return new User(
                USER_ID, "Ana", "ana@finpro.test", "hash", null, null, null, regime, null, null, 0);
    }

    private static Transaction pendingIncome(Long id, Long clientId, LocalDate date) {
        return new Transaction(
                id,
                1L,
                null,
                clientId,
                "Serviço",
                BigDecimal.valueOf(500),
                date,
                CategoryType.INCOME,
                TransactionOrigin.MANUAL,
                null,
                null,
                null,
                null,
                TransactionStatus.PENDING);
    }

    private static Transaction expense(Long id, LocalDate date, TransactionStatus status) {
        return new Transaction(
                id,
                1L,
                null,
                null,
                "Aluguel",
                BigDecimal.valueOf(1000),
                date,
                CategoryType.EXPENSE,
                TransactionOrigin.MANUAL,
                null,
                null,
                null,
                null,
                status);
    }
}
