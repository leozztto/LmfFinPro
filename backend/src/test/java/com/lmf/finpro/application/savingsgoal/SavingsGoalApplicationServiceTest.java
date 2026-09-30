package com.lmf.finpro.application.savingsgoal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lmf.finpro.domain.exception.InsufficientBalanceException;
import com.lmf.finpro.domain.exception.ResourceNotFoundException;
import com.lmf.finpro.domain.model.Account;
import com.lmf.finpro.domain.model.AccountType;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.ContributionType;
import com.lmf.finpro.domain.model.GoalContribution;
import com.lmf.finpro.domain.model.SavingsGoal;
import com.lmf.finpro.domain.model.SavingsGoalType;
import com.lmf.finpro.domain.model.TaxRegime;
import com.lmf.finpro.domain.model.Transaction;
import com.lmf.finpro.domain.model.TransactionOrigin;
import com.lmf.finpro.domain.model.TransactionStatus;
import com.lmf.finpro.domain.model.User;
import com.lmf.finpro.domain.port.out.AccountRepositoryPort;
import com.lmf.finpro.domain.port.out.GoalContributionRepositoryPort;
import com.lmf.finpro.domain.port.out.SavingsGoalRepositoryPort;
import com.lmf.finpro.domain.port.out.TransactionRepositoryPort;
import com.lmf.finpro.domain.port.out.UserRepositoryPort;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
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
class SavingsGoalApplicationServiceTest {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 26);
    private static final Long USER_ID = 10L;
    private static final Long GOAL_ID = 1L;

    @Mock private SavingsGoalRepositoryPort savingsGoalRepositoryPort;
    @Mock private GoalContributionRepositoryPort goalContributionRepositoryPort;
    @Mock private AccountRepositoryPort accountRepositoryPort;
    @Mock private TransactionRepositoryPort transactionRepositoryPort;
    @Mock private UserRepositoryPort userRepositoryPort;

    private SavingsGoalApplicationService service;

    @BeforeEach
    void setUp() {
        service =
                new SavingsGoalApplicationService(
                        savingsGoalRepositoryPort,
                        goalContributionRepositoryPort,
                        accountRepositoryPort,
                        transactionRepositoryPort,
                        userRepositoryPort,
                        Clock.fixed(TODAY.atTime(10, 0).atZone(ZONE).toInstant(), ZONE));
        lenient()
                .when(accountRepositoryPort.findAllByUserId(USER_ID))
                .thenReturn(
                        List.of(
                                new Account(
                                        5L,
                                        USER_ID,
                                        "Conta",
                                        AccountType.CHECKING,
                                        BigDecimal.ZERO,
                                        null)));
        lenient().when(transactionRepositoryPort.findAllByAccountIds(any())).thenReturn(List.of());
        lenient()
                .when(goalContributionRepositoryPort.findAllByGoalId(GOAL_ID))
                .thenReturn(List.of());
    }

    @Test
    void listSummarizesSavedAmountAndSuggestionFromPaidIncomeOfTheMonth() {
        givenGoal(goal("10000", "0.06"));
        when(transactionRepositoryPort.findAllByAccountIds(List.of(5L)))
                .thenReturn(
                        List.of(
                                income("5000", TODAY.minusDays(3), TransactionStatus.PAID, null),
                                income("2000", TODAY.plusDays(2), TransactionStatus.PENDING, null),
                                income("800", TODAY.minusDays(1), TransactionStatus.PAID, 99L),
                                income("900", TODAY.minusMonths(1), TransactionStatus.PAID, null)));
        when(goalContributionRepositoryPort.findAllByGoalId(GOAL_ID))
                .thenReturn(
                        List.of(
                                contribution(ContributionType.DEPOSIT, "100", TODAY),
                                contribution(
                                        ContributionType.DEPOSIT, "400", TODAY.minusMonths(1))));

        SavingsGoalSummary summary = service.list(USER_ID).get(0);

        assertThat(summary.savedAmount()).isEqualByComparingTo("500");
        assertThat(summary.remainingAmount()).isEqualByComparingTo("9500");
        // Só a receita paga do mês, sem transferência: 5000 × 6% = 300, menos 100 já aportados.
        assertThat(summary.monthPaidIncome()).isEqualByComparingTo("5000");
        assertThat(summary.suggestedContribution()).isEqualByComparingTo("200");
    }

    @Test
    void withdrawalLargerThanSavedIsRejected() {
        givenGoal(goal("1000", null));
        when(goalContributionRepositoryPort.findAllByGoalId(GOAL_ID))
                .thenReturn(List.of(contribution(ContributionType.DEPOSIT, "100", TODAY)));

        assertThatThrownBy(
                        () ->
                                service.addContribution(
                                        USER_ID,
                                        GOAL_ID,
                                        ContributionType.WITHDRAWAL,
                                        new BigDecimal("150"),
                                        TODAY,
                                        null))
                .isInstanceOf(InsufficientBalanceException.class);
        verify(goalContributionRepositoryPort, never()).save(any());
    }

    @Test
    void addContributionSavesWithBlankNoteAsNull() {
        givenGoal(goal("1000", null));
        when(goalContributionRepositoryPort.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        GoalContribution saved =
                service.addContribution(
                        USER_ID,
                        GOAL_ID,
                        ContributionType.DEPOSIT,
                        new BigDecimal("50"),
                        TODAY,
                        "   ");

        assertThat(saved.amount()).isEqualByComparingTo("50");
        assertThat(saved.note()).isNull();
    }

    @Test
    void deletingADepositThatWouldLeaveNegativeBalanceIsRejected() {
        givenGoal(goal("1000", null));
        GoalContribution deposit = contribution(ContributionType.DEPOSIT, "300", TODAY);
        when(goalContributionRepositoryPort.findById(7L)).thenReturn(Optional.of(deposit));
        when(goalContributionRepositoryPort.findAllByGoalId(GOAL_ID))
                .thenReturn(
                        List.of(deposit, contribution(ContributionType.WITHDRAWAL, "200", TODAY)));

        assertThatThrownBy(() -> service.deleteContribution(USER_ID, GOAL_ID, 7L))
                .isInstanceOf(InsufficientBalanceException.class);
        verify(goalContributionRepositoryPort, never()).deleteById(any());
    }

    @Test
    void applySuggestionCreatesDepositWithTheSuggestedAmount() {
        givenGoal(goal("10000", "0.06"));
        when(transactionRepositoryPort.findAllByAccountIds(List.of(5L)))
                .thenReturn(List.of(income("5000", TODAY, TransactionStatus.PAID, null)));
        when(goalContributionRepositoryPort.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.applySuggestion(USER_ID, GOAL_ID);

        ArgumentCaptor<GoalContribution> captor = ArgumentCaptor.forClass(GoalContribution.class);
        verify(goalContributionRepositoryPort).save(captor.capture());
        assertThat(captor.getValue().type()).isEqualTo(ContributionType.DEPOSIT);
        assertThat(captor.getValue().amount()).isEqualByComparingTo("300");
        assertThat(captor.getValue().contributionDate()).isEqualTo(TODAY);
        assertThat(captor.getValue().note())
                .isEqualTo("Separação de 6% das receitas recebidas em 09/2026");
    }

    @Test
    void applySuggestionWithNothingToSeparateIsRejected() {
        givenGoal(goal("10000", "0.06"));

        assertThatThrownBy(() -> service.applySuggestion(USER_ID, GOAL_ID))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void applyAutomaticContributionIfDueCreatesDepositWithAnAutomaticNote() {
        SavingsGoal goal = goal("10000", "0.06");
        when(transactionRepositoryPort.findAllByAccountIds(List.of(5L)))
                .thenReturn(List.of(income("5000", TODAY, TransactionStatus.PAID, null)));
        when(goalContributionRepositoryPort.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.applyAutomaticContributionIfDue(goal);

        ArgumentCaptor<GoalContribution> captor = ArgumentCaptor.forClass(GoalContribution.class);
        verify(goalContributionRepositoryPort).save(captor.capture());
        assertThat(captor.getValue().amount()).isEqualByComparingTo("300");
        assertThat(captor.getValue().note())
                .isEqualTo("Separação automática de 6% das receitas recebidas em 09/2026");
    }

    @Test
    void applyAutomaticContributionIfDueDoesNothingSilentlyWhenThereIsNothingToSeparate() {
        SavingsGoal goal = goal("10000", "0.06");

        service.applyAutomaticContributionIfDue(goal);

        verify(goalContributionRepositoryPort, never()).save(any());
    }

    @Test
    void findAllAutoContributeDelegatesToRepository() {
        SavingsGoal goal = goal("10000", "0.06");
        when(savingsGoalRepositoryPort.findAllAutoContribute()).thenReturn(List.of(goal));

        assertThat(service.findAllAutoContribute()).containsExactly(goal);
    }

    @Test
    void createThrowsWhenAutoContributeIsOnWithoutIncomeRate() {
        SavingsGoalCommand command =
                new SavingsGoalCommand(
                        "Caixinha", SavingsGoalType.OTHER, BigDecimal.TEN, null, null, true);

        assertThatThrownBy(() -> service.create(USER_ID, command))
                .isInstanceOf(IllegalArgumentException.class);
        verify(savingsGoalRepositoryPort, never()).save(any());
    }

    @Test
    void goalOfAnotherUserIsNotFound() {
        when(savingsGoalRepositoryPort.findById(GOAL_ID))
                .thenReturn(
                        Optional.of(
                                new SavingsGoal(
                                        GOAL_ID,
                                        99L,
                                        "Outra",
                                        SavingsGoalType.OTHER,
                                        BigDecimal.TEN,
                                        null,
                                        null,
                                        null)));

        assertThatThrownBy(() -> service.delete(USER_ID, GOAL_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void suggestedTaxRateUsesRegimeAndAverageIncomeOfPreviousThreeMonths() {
        when(userRepositoryPort.findById(USER_ID))
                .thenReturn(Optional.of(user(TaxRegime.AUTONOMO)));
        // Média de 3000/mês nos 3 meses anteriores → faixa de 15%; a receita do mês atual não
        // conta.
        when(transactionRepositoryPort.findAllByAccountIds(List.of(5L)))
                .thenReturn(
                        List.of(
                                income("3000", TODAY.minusMonths(1), TransactionStatus.PAID, null),
                                income("3000", TODAY.minusMonths(2), TransactionStatus.PAID, null),
                                income("3000", TODAY.minusMonths(3), TransactionStatus.PAID, null),
                                income("50000", TODAY, TransactionStatus.PAID, null),
                                income(
                                        "50000",
                                        TODAY.minusMonths(4),
                                        TransactionStatus.PAID,
                                        null)));

        assertThat(service.suggestedTaxRate(USER_ID)).isEqualByComparingTo("0.15");
    }

    @Test
    void suggestedTaxRateForMeiIsTheFixedReferenceRate() {
        when(userRepositoryPort.findById(USER_ID)).thenReturn(Optional.of(user(TaxRegime.MEI)));

        assertThat(service.suggestedTaxRate(USER_ID)).isEqualByComparingTo("0.06");
    }

    private void givenGoal(SavingsGoal goal) {
        lenient().when(savingsGoalRepositoryPort.findById(GOAL_ID)).thenReturn(Optional.of(goal));
        lenient()
                .when(savingsGoalRepositoryPort.findAllByUserId(USER_ID))
                .thenReturn(List.of(goal));
    }

    private static SavingsGoal goal(String target, String rate) {
        return new SavingsGoal(
                GOAL_ID,
                USER_ID,
                "Caixinha do imposto",
                SavingsGoalType.TAX_RESERVE,
                new BigDecimal(target),
                null,
                rate == null ? null : new BigDecimal(rate),
                null);
    }

    private static GoalContribution contribution(
            ContributionType type, String amount, LocalDate date) {
        return new GoalContribution(7L, GOAL_ID, type, new BigDecimal(amount), date, null, null);
    }

    private static Transaction income(
            String amount, LocalDate date, TransactionStatus status, Long transferId) {
        return new Transaction(
                null,
                5L,
                null,
                null,
                "Receita",
                new BigDecimal(amount),
                date,
                CategoryType.INCOME,
                TransactionOrigin.MANUAL,
                null,
                transferId,
                null,
                null,
                status);
    }

    private static User user(TaxRegime regime) {
        return new User(
                USER_ID, "Ana", "ana@finpro.test", "hash", null, null, null, regime, null, null, 0);
    }
}
