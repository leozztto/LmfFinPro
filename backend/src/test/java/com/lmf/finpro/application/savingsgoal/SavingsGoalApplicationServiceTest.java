package com.lmf.finpro.application.savingsgoal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lmf.finpro.application.support.HouseholdTaxProfile;
import com.lmf.finpro.application.transfer.TransferApplicationService;
import com.lmf.finpro.application.transfer.TransferResult;
import com.lmf.finpro.domain.exception.EntityHasLinkedRecordsException;
import com.lmf.finpro.domain.exception.InsufficientBalanceException;
import com.lmf.finpro.domain.exception.ResourceNotFoundException;
import com.lmf.finpro.domain.exception.SameAccountTransferException;
import com.lmf.finpro.domain.model.Account;
import com.lmf.finpro.domain.model.AccountScope;
import com.lmf.finpro.domain.model.AccountType;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.ContributionType;
import com.lmf.finpro.domain.model.Currency;
import com.lmf.finpro.domain.model.GoalContribution;
import com.lmf.finpro.domain.model.SavingsGoal;
import com.lmf.finpro.domain.model.SavingsGoalType;
import com.lmf.finpro.domain.model.TaxRegime;
import com.lmf.finpro.domain.model.Transaction;
import com.lmf.finpro.domain.model.TransactionOrigin;
import com.lmf.finpro.domain.model.TransactionStatus;
import com.lmf.finpro.domain.model.Transfer;
import com.lmf.finpro.domain.model.User;
import com.lmf.finpro.domain.port.out.AccountRepositoryPort;
import com.lmf.finpro.domain.port.out.GoalContributionRepositoryPort;
import com.lmf.finpro.domain.port.out.SavingsGoalRepositoryPort;
import com.lmf.finpro.domain.port.out.TransactionRepositoryPort;
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
    // Quem está logado: distinto do grupo (USER_ID), como na vida real.
    private static final Long ACTOR_ID = 99L;
    private static final Long GOAL_ID = 1L;
    private static final Long ACCOUNT_ID = 5L;
    private static final Long FUNDING_ACCOUNT_ID = 6L;
    private static final Long TRANSFER_ID = 77L;

    @Mock private SavingsGoalRepositoryPort savingsGoalRepositoryPort;
    @Mock private GoalContributionRepositoryPort goalContributionRepositoryPort;
    @Mock private AccountRepositoryPort accountRepositoryPort;
    @Mock private TransactionRepositoryPort transactionRepositoryPort;
    @Mock private HouseholdTaxProfile householdTaxProfile;
    @Mock private TransferApplicationService transferApplicationService;

    private SavingsGoalApplicationService service;

    @BeforeEach
    void setUp() {
        service =
                new SavingsGoalApplicationService(
                        savingsGoalRepositoryPort,
                        goalContributionRepositoryPort,
                        accountRepositoryPort,
                        transactionRepositoryPort,
                        householdTaxProfile,
                        transferApplicationService,
                        Clock.fixed(TODAY.atTime(10, 0).atZone(ZONE).toInstant(), ZONE));
        lenient()
                .when(accountRepositoryPort.findAllByHouseholdId(USER_ID))
                .thenReturn(
                        List.of(
                                new Account(
                                        ACCOUNT_ID,
                                        USER_ID,
                                        "Conta",
                                        AccountType.CHECKING,
                                        BigDecimal.ZERO,
                                        null)));
        lenient().when(transactionRepositoryPort.findAllByAccountIds(any())).thenReturn(List.of());
        lenient()
                .when(goalContributionRepositoryPort.findAllByGoalId(GOAL_ID))
                .thenReturn(List.of());
        // Transferência real por trás de cada aporte/resgate: por padrão, devolve um id fixo com as
        // contas/valor/data recebidos, pra testes que só precisam de um GoalContribution válido.
        lenient()
                .when(
                        transferApplicationService.create(
                                any(), any(), any(), any(), any(), any(), any(), any()))
                .thenAnswer(
                        invocation ->
                                new TransferResult(
                                        new Transfer(
                                                TRANSFER_ID,
                                                USER_ID,
                                                invocation.getArgument(2),
                                                invocation.getArgument(3),
                                                invocation.getArgument(4),
                                                invocation.getArgument(5),
                                                invocation.getArgument(6),
                                                null),
                                        100L,
                                        101L));
    }

    @Test
    void listSummarizesSavedAmountAndSuggestionFromPaidIncomeOfTheMonth() {
        givenGoal(goal("10000", "0.06"));
        when(transactionRepositoryPort.findAllByAccountIds(List.of(ACCOUNT_ID)))
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
                                        ACTOR_ID,
                                        GOAL_ID,
                                        ContributionType.WITHDRAWAL,
                                        new BigDecimal("150"),
                                        TODAY,
                                        null))
                .isInstanceOf(InsufficientBalanceException.class);
        verify(goalContributionRepositoryPort, never()).save(any());
        verify(transferApplicationService, never())
                .create(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void addContributionSavesWithBlankNoteAsNull() {
        givenGoal(goal("1000", null));
        when(goalContributionRepositoryPort.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        GoalContribution saved =
                service.addContribution(
                        USER_ID,
                        ACTOR_ID,
                        GOAL_ID,
                        ContributionType.DEPOSIT,
                        new BigDecimal("50"),
                        TODAY,
                        "   ");

        assertThat(saved.amount()).isEqualByComparingTo("50");
        assertThat(saved.note()).isNull();
        assertThat(saved.transferId()).isEqualTo(TRANSFER_ID);
    }

    @Test
    void depositTransfersFromFundingAccountToReserveAccount() {
        givenGoal(goal("1000", null));
        when(goalContributionRepositoryPort.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.addContribution(
                USER_ID,
                ACTOR_ID,
                GOAL_ID,
                ContributionType.DEPOSIT,
                new BigDecimal("50"),
                TODAY,
                null);

        verify(transferApplicationService)
                .create(
                        eq(USER_ID),
                        eq(ACTOR_ID),
                        eq(FUNDING_ACCOUNT_ID),
                        eq(ACCOUNT_ID),
                        eq(new BigDecimal("50")),
                        eq(TODAY),
                        eq("Aporte na meta \"Caixinha do imposto\""),
                        isNull());
    }

    @Test
    void withdrawalTransfersFromReserveAccountToFundingAccount() {
        givenGoal(goal("1000", null));
        when(goalContributionRepositoryPort.findAllByGoalId(GOAL_ID))
                .thenReturn(List.of(contribution(ContributionType.DEPOSIT, "300", TODAY)));
        when(goalContributionRepositoryPort.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.addContribution(
                USER_ID,
                ACTOR_ID,
                GOAL_ID,
                ContributionType.WITHDRAWAL,
                new BigDecimal("100"),
                TODAY,
                null);

        verify(transferApplicationService)
                .create(
                        eq(USER_ID),
                        eq(ACTOR_ID),
                        eq(ACCOUNT_ID),
                        eq(FUNDING_ACCOUNT_ID),
                        eq(new BigDecimal("100")),
                        eq(TODAY),
                        eq("Resgate da meta \"Caixinha do imposto\""),
                        isNull());
    }

    @Test
    void deletingADepositThatWouldLeaveNegativeBalanceIsRejected() {
        givenGoal(goal("1000", null));
        GoalContribution deposit = contribution(ContributionType.DEPOSIT, "300", TODAY);
        when(goalContributionRepositoryPort.findById(7L)).thenReturn(Optional.of(deposit));
        when(goalContributionRepositoryPort.findAllByGoalId(GOAL_ID))
                .thenReturn(
                        List.of(deposit, contribution(ContributionType.WITHDRAWAL, "200", TODAY)));

        assertThatThrownBy(() -> service.deleteContribution(USER_ID, ACTOR_ID, GOAL_ID, 7L))
                .isInstanceOf(InsufficientBalanceException.class);
        verify(transferApplicationService, never()).delete(any(), any(), any());
    }

    @Test
    void deleteContributionDeletesTheLinkedTransfer() {
        givenGoal(goal("1000", null));
        GoalContribution deposit = contribution(ContributionType.DEPOSIT, "300", TODAY);
        when(goalContributionRepositoryPort.findById(7L)).thenReturn(Optional.of(deposit));
        when(goalContributionRepositoryPort.findAllByGoalId(GOAL_ID)).thenReturn(List.of(deposit));

        service.deleteContribution(USER_ID, ACTOR_ID, GOAL_ID, 7L);

        verify(transferApplicationService).delete(USER_ID, ACTOR_ID, TRANSFER_ID);
    }

    @Test
    void applySuggestionCreatesDepositWithTheSuggestedAmount() {
        givenGoal(goal("10000", "0.06"));
        when(transactionRepositoryPort.findAllByAccountIds(List.of(ACCOUNT_ID)))
                .thenReturn(List.of(income("5000", TODAY, TransactionStatus.PAID, null)));
        when(goalContributionRepositoryPort.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.applySuggestion(USER_ID, ACTOR_ID, GOAL_ID);

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

        assertThatThrownBy(() -> service.applySuggestion(USER_ID, ACTOR_ID, GOAL_ID))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void applyAutomaticContributionIfDueCreatesDepositWithAnAutomaticNote() {
        SavingsGoal goal = goal("10000", "0.06");
        when(transactionRepositoryPort.findAllByAccountIds(List.of(ACCOUNT_ID)))
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

        assertThatThrownBy(() -> service.create(USER_ID, ACCOUNT_ID, FUNDING_ACCOUNT_ID, command))
                .isInstanceOf(IllegalArgumentException.class);
        verify(savingsGoalRepositoryPort, never()).save(any());
    }

    @Test
    void createSavesGoalWithTheGivenAccountLinks() {
        givenAccounts(Currency.BRL, Currency.BRL);
        when(savingsGoalRepositoryPort.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        SavingsGoalCommand command =
                new SavingsGoalCommand(
                        "Viagem", SavingsGoalType.VACATION, BigDecimal.TEN, null, null, false);

        SavingsGoalSummary summary =
                service.create(USER_ID, ACCOUNT_ID, FUNDING_ACCOUNT_ID, command);

        assertThat(summary.goal().accountId()).isEqualTo(ACCOUNT_ID);
        assertThat(summary.goal().fundingAccountId()).isEqualTo(FUNDING_ACCOUNT_ID);
    }

    @Test
    void createRejectsWhenReserveAndFundingAccountsAreTheSame() {
        SavingsGoalCommand command =
                new SavingsGoalCommand(
                        "Viagem", SavingsGoalType.VACATION, BigDecimal.TEN, null, null, false);

        assertThatThrownBy(() -> service.create(USER_ID, ACCOUNT_ID, ACCOUNT_ID, command))
                .isInstanceOf(SameAccountTransferException.class);
        verify(savingsGoalRepositoryPort, never()).save(any());
    }

    @Test
    void createRejectsWhenAccountsHaveDifferentCurrencies() {
        givenAccounts(Currency.BRL, Currency.USD);
        SavingsGoalCommand command =
                new SavingsGoalCommand(
                        "Viagem", SavingsGoalType.VACATION, BigDecimal.TEN, null, null, false);

        assertThatThrownBy(() -> service.create(USER_ID, ACCOUNT_ID, FUNDING_ACCOUNT_ID, command))
                .isInstanceOf(IllegalArgumentException.class);
        verify(savingsGoalRepositoryPort, never()).save(any());
    }

    @Test
    void createRejectsWhenReserveAccountIsNotOfReserveType() {
        givenAccounts(AccountType.CHECKING, Currency.BRL, Currency.BRL);
        SavingsGoalCommand command =
                new SavingsGoalCommand(
                        "Viagem", SavingsGoalType.VACATION, BigDecimal.TEN, null, null, false);

        assertThatThrownBy(() -> service.create(USER_ID, ACCOUNT_ID, FUNDING_ACCOUNT_ID, command))
                .isInstanceOf(IllegalArgumentException.class);
        verify(savingsGoalRepositoryPort, never()).save(any());
    }

    @Test
    void deleteRejectsWhenGoalHasSavedBalance() {
        givenGoal(goal("1000", null));
        when(goalContributionRepositoryPort.findAllByGoalId(GOAL_ID))
                .thenReturn(List.of(contribution(ContributionType.DEPOSIT, "300", TODAY)));

        assertThatThrownBy(() -> service.delete(USER_ID, GOAL_ID))
                .isInstanceOf(EntityHasLinkedRecordsException.class);
        verify(savingsGoalRepositoryPort, never()).deleteById(any());
    }

    @Test
    void deleteSucceedsWhenSavedBalanceIsZero() {
        givenGoal(goal("1000", null));

        service.delete(USER_ID, GOAL_ID);

        verify(savingsGoalRepositoryPort).deleteById(GOAL_ID);
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
                                        null,
                                        false,
                                        1L,
                                        2L)));

        assertThatThrownBy(() -> service.delete(USER_ID, GOAL_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void suggestedTaxRateUsesRegimeAndAverageIncomeOfPreviousThreeMonths() {
        when(householdTaxProfile.regimeOf(USER_ID)).thenReturn(TaxRegime.AUTONOMO);
        // Média de 3000/mês nos 3 meses anteriores → faixa de 15%; a receita do mês atual não
        // conta.
        when(transactionRepositoryPort.findAllByAccountIds(List.of(ACCOUNT_ID)))
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
        when(householdTaxProfile.regimeOf(USER_ID)).thenReturn(TaxRegime.MEI);

        assertThat(service.suggestedTaxRate(USER_ID)).isEqualByComparingTo("0.06");
    }

    private void givenGoal(SavingsGoal goal) {
        lenient().when(savingsGoalRepositoryPort.findById(GOAL_ID)).thenReturn(Optional.of(goal));
        lenient()
                .when(savingsGoalRepositoryPort.findAllByHouseholdId(USER_ID))
                .thenReturn(List.of(goal));
    }

    private void givenAccounts(Currency reserveCurrency, Currency fundingCurrency) {
        givenAccounts(AccountType.RESERVE, reserveCurrency, fundingCurrency);
    }

    private void givenAccounts(
            AccountType reserveAccountType, Currency reserveCurrency, Currency fundingCurrency) {
        lenient()
                .when(accountRepositoryPort.findById(ACCOUNT_ID))
                .thenReturn(Optional.of(account(ACCOUNT_ID, reserveAccountType, reserveCurrency)));
        lenient()
                .when(accountRepositoryPort.findById(FUNDING_ACCOUNT_ID))
                .thenReturn(
                        Optional.of(
                                account(
                                        FUNDING_ACCOUNT_ID,
                                        AccountType.CHECKING,
                                        fundingCurrency)));
    }

    private static Account account(Long id, AccountType type, Currency currency) {
        return new Account(
                id,
                USER_ID,
                "Conta " + id,
                type,
                BigDecimal.ZERO,
                null,
                AccountScope.PERSONAL,
                currency);
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
                null,
                false,
                ACCOUNT_ID,
                FUNDING_ACCOUNT_ID);
    }

    private static GoalContribution contribution(
            ContributionType type, String amount, LocalDate date) {
        return new GoalContribution(
                7L, GOAL_ID, type, new BigDecimal(amount), date, null, null, TRANSFER_ID);
    }

    private static Transaction income(
            String amount, LocalDate date, TransactionStatus status, Long transferId) {
        return new Transaction(
                null,
                ACCOUNT_ID,
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
