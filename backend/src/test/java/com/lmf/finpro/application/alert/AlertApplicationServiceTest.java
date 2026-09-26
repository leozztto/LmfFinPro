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
import com.lmf.finpro.domain.model.Account;
import com.lmf.finpro.domain.model.AccountType;
import com.lmf.finpro.domain.model.AlertDigest;
import com.lmf.finpro.domain.model.AlertType;
import com.lmf.finpro.domain.model.Budget;
import com.lmf.finpro.domain.model.Category;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.NotificationPreferences;
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
import com.lmf.finpro.domain.port.out.NotificationPreferencesRepositoryPort;
import com.lmf.finpro.domain.port.out.SentAlertRepositoryPort;
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

    @Mock private UserRepositoryPort userRepositoryPort;
    @Mock private NotificationPreferencesRepositoryPort notificationPreferencesRepositoryPort;
    @Mock private SentAlertRepositoryPort sentAlertRepositoryPort;
    @Mock private AccountRepositoryPort accountRepositoryPort;
    @Mock private TransactionRepositoryPort transactionRepositoryPort;
    @Mock private BudgetRepositoryPort budgetRepositoryPort;
    @Mock private BudgetApplicationService budgetApplicationService;
    @Mock private CategoryRepositoryPort categoryRepositoryPort;
    @Mock private TaxEstimateRepositoryPort taxEstimateRepositoryPort;
    @Mock private AlertMailerPort alertMailerPort;

    @BeforeEach
    void setUp() {
        lenient()
                .when(notificationPreferencesRepositoryPort.findByUserId(USER_ID))
                .thenReturn(Optional.empty());
        lenient()
                .when(accountRepositoryPort.findAllByUserId(USER_ID))
                .thenReturn(
                        List.of(
                                new Account(
                                        1L,
                                        USER_ID,
                                        "Conta",
                                        AccountType.CHECKING,
                                        BigDecimal.ZERO,
                                        null)));
        lenient().when(transactionRepositoryPort.findAllByAccountIds(any())).thenReturn(List.of());
        lenient().when(budgetRepositoryPort.findAllByUserId(USER_ID)).thenReturn(List.of());
        lenient().when(sentAlertRepositoryPort.exists(anyLong(), any(), any())).thenReturn(false);
        lenient().when(taxEstimateRepositoryPort.findAllByUserId(USER_ID)).thenReturn(List.of());
        lenient()
                .when(categoryRepositoryPort.findById(5L))
                .thenReturn(
                        Optional.of(
                                new Category(
                                        5L, USER_ID, "Mercado", CategoryType.EXPENSE, null, null)));
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
        when(budgetRepositoryPort.findAllByUserId(USER_ID))
                .thenReturn(
                        List.of(
                                new Budget(
                                        7L,
                                        USER_ID,
                                        5L,
                                        YearMonth.from(TODAY).minusMonths(1),
                                        BigDecimal.valueOf(1000))));

        assertThat(service(TODAY).sendAlertsTo(user(TaxRegime.AUTONOMO))).isFalse();
    }

    @Test
    void dasReminderForMeiIncludesEstimatedValueOfTheCompetence() {
        YearMonth competence = YearMonth.of(2026, 8);
        when(taxEstimateRepositoryPort.findAllByUserId(USER_ID))
                .thenReturn(
                        List.of(
                                new TaxEstimate(
                                        1L,
                                        USER_ID,
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
                        Optional.of(new NotificationPreferences(USER_ID, false, 3, false, false)));
        lenient()
                .when(transactionRepositoryPort.findAllByAccountIds(List.of(1L)))
                .thenReturn(List.of(expense(1L, TODAY.plusDays(1), TransactionStatus.PENDING)));
        givenBudgetWithSpent("1200");

        assertThat(service(TODAY).sendAlertsTo(user(TaxRegime.MEI))).isFalse();
        verify(alertMailerPort, never()).sendDigest(any(), any(), any());
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
                notificationPreferencesRepositoryPort,
                sentAlertRepositoryPort,
                accountRepositoryPort,
                transactionRepositoryPort,
                budgetRepositoryPort,
                budgetApplicationService,
                categoryRepositoryPort,
                taxEstimateRepositoryPort,
                alertMailerPort,
                Clock.fixed(today.atTime(8, 0).atZone(ZONE).toInstant(), ZONE));
    }

    private void givenBudgetWithSpent(String spent) {
        Budget budget =
                new Budget(7L, USER_ID, 5L, YearMonth.from(TODAY), BigDecimal.valueOf(1000));
        lenient().when(budgetRepositoryPort.findAllByUserId(USER_ID)).thenReturn(List.of(budget));
        lenient()
                .when(budgetApplicationService.calculateSpent(budget))
                .thenReturn(new BigDecimal(spent));
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
