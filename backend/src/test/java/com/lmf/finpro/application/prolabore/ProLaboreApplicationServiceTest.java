package com.lmf.finpro.application.prolabore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.lmf.finpro.application.account.AccountApplicationService;
import com.lmf.finpro.application.support.HouseholdTaxProfile;
import com.lmf.finpro.domain.model.Account;
import com.lmf.finpro.domain.model.AccountScope;
import com.lmf.finpro.domain.model.AccountType;
import com.lmf.finpro.domain.model.Category;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.ContributionType;
import com.lmf.finpro.domain.model.GoalContribution;
import com.lmf.finpro.domain.model.ProLaboreCalculationBase;
import com.lmf.finpro.domain.model.ProLaboreSettings;
import com.lmf.finpro.domain.model.ProLaboreTaxMode;
import com.lmf.finpro.domain.model.ProLaboreWithholdingMode;
import com.lmf.finpro.domain.model.SavingsGoal;
import com.lmf.finpro.domain.model.SavingsGoalType;
import com.lmf.finpro.domain.model.TaxRegime;
import com.lmf.finpro.domain.model.Transaction;
import com.lmf.finpro.domain.model.TransactionOrigin;
import com.lmf.finpro.domain.model.TransactionStatus;
import com.lmf.finpro.domain.model.Transfer;
import com.lmf.finpro.domain.model.User;
import com.lmf.finpro.domain.port.out.AccountRepositoryPort;
import com.lmf.finpro.domain.port.out.CategoryRepositoryPort;
import com.lmf.finpro.domain.port.out.GoalContributionRepositoryPort;
import com.lmf.finpro.domain.port.out.ProLaboreSettingsRepositoryPort;
import com.lmf.finpro.domain.port.out.SavingsGoalRepositoryPort;
import com.lmf.finpro.domain.port.out.TransactionRepositoryPort;
import com.lmf.finpro.domain.port.out.TransferRepositoryPort;
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
class ProLaboreApplicationServiceTest {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 26);
    private static final Long USER_ID = 10L;
    private static final Account BUSINESS =
            new Account(
                    1L,
                    USER_ID,
                    "PJ",
                    AccountType.CHECKING,
                    BigDecimal.ZERO,
                    null,
                    AccountScope.BUSINESS);
    private static final Account PERSONAL =
            new Account(
                    2L,
                    USER_ID,
                    "PF",
                    AccountType.CHECKING,
                    BigDecimal.ZERO,
                    null,
                    AccountScope.PERSONAL);

    @Mock private AccountRepositoryPort accountRepositoryPort;
    @Mock private AccountApplicationService accountApplicationService;
    @Mock private TransactionRepositoryPort transactionRepositoryPort;
    @Mock private TransferRepositoryPort transferRepositoryPort;
    @Mock private SavingsGoalRepositoryPort savingsGoalRepositoryPort;
    @Mock private GoalContributionRepositoryPort goalContributionRepositoryPort;
    @Mock private HouseholdTaxProfile householdTaxProfile;
    @Mock private ProLaboreSettingsRepositoryPort proLaboreSettingsRepositoryPort;
    @Mock private CategoryRepositoryPort categoryRepositoryPort;

    private ProLaboreApplicationService service;

    @BeforeEach
    void setUp() {
        service =
                new ProLaboreApplicationService(
                        accountRepositoryPort,
                        accountApplicationService,
                        transactionRepositoryPort,
                        transferRepositoryPort,
                        savingsGoalRepositoryPort,
                        goalContributionRepositoryPort,
                        householdTaxProfile,
                        proLaboreSettingsRepositoryPort,
                        categoryRepositoryPort,
                        Clock.fixed(TODAY.atTime(10, 0).atZone(ZONE).toInstant(), ZONE));
        lenient()
                .when(accountRepositoryPort.findAllByHouseholdId(USER_ID))
                .thenReturn(List.of(BUSINESS, PERSONAL));
        lenient()
                .when(accountApplicationService.calculateCurrentBalance(BUSINESS))
                .thenReturn(new BigDecimal("20000"));
        lenient().when(transactionRepositoryPort.findAllByAccountIds(any())).thenReturn(List.of());
        lenient().when(transferRepositoryPort.findAllByHouseholdId(USER_ID)).thenReturn(List.of());
        lenient()
                .when(savingsGoalRepositoryPort.findAllByHouseholdId(USER_ID))
                .thenReturn(List.of());
        lenient().when(householdTaxProfile.regimeOf(USER_ID)).thenReturn(TaxRegime.MEI);
        lenient()
                .when(proLaboreSettingsRepositoryPort.findByHouseholdId(USER_ID))
                .thenReturn(Optional.empty());
    }

    @Test
    void availableDiscountsPendingBillsTaxAndCashCushionFromBusinessBalance() {
        when(transactionRepositoryPort.findAllByAccountIds(List.of(1L)))
                .thenReturn(
                        List.of(
                                tx(CategoryType.INCOME, "10000", TODAY.minusDays(5), true, null),
                                tx(CategoryType.INCOME, "4000", TODAY.plusDays(2), false, null),
                                tx(
                                        CategoryType.EXPENSE,
                                        "1500",
                                        LocalDate.of(2026, 9, 30),
                                        false,
                                        null),
                                tx(
                                        CategoryType.EXPENSE,
                                        "200",
                                        LocalDate.of(2026, 9, 2),
                                        false,
                                        null),
                                tx(
                                        CategoryType.EXPENSE,
                                        "800",
                                        LocalDate.of(2026, 10, 5),
                                        false,
                                        null),
                                tx(
                                        CategoryType.EXPENSE,
                                        "3000",
                                        LocalDate.of(2026, 6, 10),
                                        true,
                                        null),
                                tx(
                                        CategoryType.EXPENSE,
                                        "3000",
                                        LocalDate.of(2026, 7, 10),
                                        true,
                                        null),
                                tx(
                                        CategoryType.EXPENSE,
                                        "3000",
                                        LocalDate.of(2026, 8, 10),
                                        true,
                                        null),
                                tx(
                                        CategoryType.EXPENSE,
                                        "9999",
                                        LocalDate.of(2026, 5, 10),
                                        true,
                                        null),
                                tx(CategoryType.EXPENSE, "5000", TODAY, true, 77L)));

        ProLaboreSummary summary = service.summary(USER_ID);

        assertThat(summary.hasBusinessAccounts()).isTrue();
        assertThat(summary.settings().calculationBase())
                .isEqualTo(ProLaboreCalculationBase.MONTH_INCOME);
        assertThat(summary.businessBalance()).isEqualByComparingTo("20000");
        // Pendentes até o fim do mês (inclusive a de 02/09), sem a de outubro.
        assertThat(summary.pendingBusinessExpenses()).isEqualByComparingTo("1700");
        // Nada pago em setembro + 1700 pendentes (transferência fica de fora).
        assertThat(summary.monthBusinessExpenses()).isEqualByComparingTo("1700");
        // A lista mostra exatamente as despesas somadas no custo do mês, da mais recente à mais
        // antiga; a de 02/09 está atrasada.
        assertThat(summary.businessExpenses())
                .extracting(ProLaboreSummary.BusinessExpense::amount)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("1500"), new BigDecimal("200"));
        assertThat(summary.businessExpenses())
                .extracting(ProLaboreSummary.BusinessExpense::overdue)
                .containsExactly(false, true);
        assertThat(summary.businessExpenses().get(0).accountName()).isEqualTo("PJ");
        // Só a receita PJ recebida: 10000 × 6% (MEI).
        assertThat(summary.monthBusinessIncome()).isEqualByComparingTo("10000");
        assertThat(summary.taxRate()).isEqualByComparingTo("0.06");
        // Média de junho a agosto (maio e transferências ficam de fora).
        assertThat(summary.averageMonthlyBusinessExpense()).isEqualByComparingTo("3000");
        // Base padrão (receitas do mês): 10000 − 1700 − 600 − 1000 (reserva de 10%).
        assertThat(summary.result().availableToWithdraw()).isEqualByComparingTo("6700");
        assertThat(summary.suggestedFromAccountId()).isEqualTo(1L);
        assertThat(summary.suggestedToAccountId()).isEqualTo(2L);
    }

    @Test
    void businessExpensesListPaidOnesOfTheMonthWithCategoryName() {
        Transaction paid =
                new Transaction(
                        40L,
                        1L,
                        7L,
                        null,
                        "Contador",
                        new BigDecimal("450"),
                        LocalDate.of(2026, 9, 10),
                        CategoryType.EXPENSE,
                        TransactionOrigin.MANUAL,
                        null,
                        null,
                        null,
                        null,
                        TransactionStatus.PAID);
        when(transactionRepositoryPort.findAllByAccountIds(List.of(1L)))
                .thenReturn(
                        List.of(
                                paid,
                                tx(
                                        CategoryType.EXPENSE,
                                        "300",
                                        LocalDate.of(2026, 8, 10),
                                        true,
                                        null)));
        when(categoryRepositoryPort.findAllVisibleToUser(USER_ID))
                .thenReturn(
                        List.of(
                                new Category(
                                        7L,
                                        USER_ID,
                                        "Serviços",
                                        CategoryType.EXPENSE,
                                        null,
                                        null)));

        ProLaboreSummary summary = service.summary(USER_ID);

        assertThat(summary.businessExpenses()).hasSize(1);
        ProLaboreSummary.BusinessExpense expense = summary.businessExpenses().get(0);
        assertThat(expense.transactionId()).isEqualTo(40L);
        assertThat(expense.description()).isEqualTo("Contador");
        assertThat(expense.paid()).isTrue();
        assertThat(expense.overdue()).isFalse();
        assertThat(expense.categoryName()).isEqualTo("Serviços");
        assertThat(summary.monthBusinessExpenses()).isEqualByComparingTo("450");
    }

    @Test
    void currentBalanceBaseUsesTheConfiguredCashCushion() {
        givenSettings(
                new ProLaboreSettings(
                        USER_ID,
                        ProLaboreCalculationBase.CURRENT_BALANCE,
                        3,
                        new BigDecimal("0.10"),
                        ProLaboreTaxMode.AUTOMATIC,
                        null,
                        null,
                        ProLaboreWithholdingMode.AUTOMATIC,
                        null));
        when(transactionRepositoryPort.findAllByAccountIds(List.of(1L)))
                .thenReturn(
                        List.of(
                                tx(
                                        CategoryType.EXPENSE,
                                        "3000",
                                        LocalDate.of(2026, 8, 1),
                                        true,
                                        null)));

        ProLaboreSummary summary = service.summary(USER_ID);

        // Média de 1000/mês (3000 ÷ 3) × 3 meses de colchão; saldo 20000 − 3000.
        assertThat(summary.averageMonthlyBusinessExpense()).isEqualByComparingTo("1000");
        assertThat(summary.result().cashCushion()).isEqualByComparingTo("3000");
        assertThat(summary.result().availableToWithdraw()).isEqualByComparingTo("17000");
    }

    @Test
    void manualTaxModeUsesTheInformedRate() {
        givenSettings(
                new ProLaboreSettings(
                        USER_ID,
                        ProLaboreCalculationBase.MONTH_INCOME,
                        1,
                        BigDecimal.ZERO,
                        ProLaboreTaxMode.MANUAL,
                        new BigDecimal("0.15"),
                        null,
                        ProLaboreWithholdingMode.AUTOMATIC,
                        null));
        when(transactionRepositoryPort.findAllByAccountIds(List.of(1L)))
                .thenReturn(List.of(tx(CategoryType.INCOME, "10000", TODAY, true, null)));

        ProLaboreSummary summary = service.summary(USER_ID);

        assertThat(summary.taxRate()).isEqualByComparingTo("0.15");
        assertThat(summary.result().taxReserve()).isEqualByComparingTo("1500");
        // MEI: sem retenção de INSS/IRRF no modo automático.
        assertThat(summary.result().withholdingApplied()).isFalse();
        assertThat(summary.result().availableToWithdraw()).isEqualByComparingTo("8500");
    }

    @Test
    void automaticWithholdingAppliesToLucroPresumidoWithEmployerInss() {
        when(householdTaxProfile.regimeOf(USER_ID)).thenReturn(TaxRegime.LUCRO_PRESUMIDO);
        when(transactionRepositoryPort.findAllByAccountIds(List.of(1L)))
                .thenReturn(List.of(tx(CategoryType.INCOME, "10000", TODAY, true, null)));

        ProLaboreSummary summary = service.summary(USER_ID);

        // Imposto de referência 11,33%; reserva 10%: orçamento 10000 − 1133 − 1000 = 7867,
        // que cobre bruto + 20% patronal.
        assertThat(summary.result().monthBudget()).isEqualByComparingTo("7867");
        assertThat(summary.result().withholdingApplied()).isTrue();
        assertThat(summary.result().employerInssRate()).isEqualByComparingTo("0.20");
        assertThat(summary.result().payroll().gross()).isEqualByComparingTo("6555.83");
        assertThat(summary.result().payroll().net()).isLessThan(summary.result().payroll().gross());
    }

    @Test
    void taxReserveUsesTheTaxBoxRateAndKeepsWhatIsAlreadySaved() {
        when(transactionRepositoryPort.findAllByAccountIds(List.of(1L)))
                .thenReturn(List.of(tx(CategoryType.INCOME, "10000", TODAY, true, null)));
        SavingsGoal taxBox =
                new SavingsGoal(
                        5L,
                        USER_ID,
                        "Imposto",
                        SavingsGoalType.TAX_RESERVE,
                        new BigDecimal("50000"),
                        null,
                        new BigDecimal("0.10"),
                        null,
                        false,
                        20L,
                        21L);
        when(savingsGoalRepositoryPort.findAllByHouseholdId(USER_ID)).thenReturn(List.of(taxBox));
        when(goalContributionRepositoryPort.findAllByGoalId(5L))
                .thenReturn(
                        List.of(
                                new GoalContribution(
                                        1L,
                                        5L,
                                        ContributionType.DEPOSIT,
                                        new BigDecimal("5000"),
                                        TODAY,
                                        null,
                                        null,
                                        9L)));

        ProLaboreSummary summary = service.summary(USER_ID);

        // Modo automático: a alíquota da caixinha tem prioridade sobre a do regime.
        assertThat(summary.taxRate()).isEqualByComparingTo("0.10");
        assertThat(summary.taxReserveSaved()).isEqualByComparingTo("5000");
        assertThat(summary.result().taxOnMonthIncome()).isEqualByComparingTo("1000");
    }

    @Test
    void withdrawalsAreTransfersFromBusinessToPersonalInTheMonth() {
        when(transferRepositoryPort.findAllByHouseholdId(USER_ID))
                .thenReturn(
                        List.of(
                                transfer(1L, 1L, 2L, "2000", TODAY.minusDays(1)),
                                transfer(2L, 1L, 2L, "1500", TODAY.minusDays(10)),
                                transfer(3L, 2L, 1L, "700", TODAY),
                                transfer(4L, 1L, 2L, "900", TODAY.minusMonths(1))));

        ProLaboreSummary summary = service.summary(USER_ID);

        assertThat(summary.withdrawnThisMonth()).isEqualByComparingTo("3500");
        assertThat(summary.withdrawals())
                .extracting(ProLaboreSummary.Withdrawal::transferId)
                .containsExactly(1L, 2L);
        assertThat(summary.withdrawals().get(0).fromAccountName()).isEqualTo("PJ");
        assertThat(summary.withdrawals().get(0).toAccountName()).isEqualTo("PF");
    }

    @Test
    void withoutBusinessAccountsThereIsNothingToWithdraw() {
        when(accountRepositoryPort.findAllByHouseholdId(USER_ID)).thenReturn(List.of(PERSONAL));

        ProLaboreSummary summary = service.summary(USER_ID);

        assertThat(summary.hasBusinessAccounts()).isFalse();
        assertThat(summary.result().availableToWithdraw()).isEqualByComparingTo("0");
        assertThat(summary.suggestedFromAccountId()).isNull();
    }

    private void givenSettings(ProLaboreSettings settings) {
        when(proLaboreSettingsRepositoryPort.findByHouseholdId(USER_ID))
                .thenReturn(Optional.of(settings));
    }

    private static Transaction tx(
            CategoryType type, String amount, LocalDate date, boolean paid, Long transferId) {
        return new Transaction(
                null,
                1L,
                null,
                null,
                "Movimento",
                new BigDecimal(amount),
                date,
                type,
                TransactionOrigin.MANUAL,
                null,
                transferId,
                null,
                null,
                paid ? TransactionStatus.PAID : TransactionStatus.PENDING);
    }

    private static Transfer transfer(Long id, Long from, Long to, String amount, LocalDate date) {
        return new Transfer(id, USER_ID, from, to, new BigDecimal(amount), date, null, null);
    }

    private static User user(TaxRegime regime) {
        return new User(
                USER_ID, "Ana", "ana@finpro.test", "hash", null, null, null, regime, null, null, 0);
    }
}
