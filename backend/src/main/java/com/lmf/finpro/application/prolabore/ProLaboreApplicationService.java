package com.lmf.finpro.application.prolabore;

import com.lmf.finpro.application.account.AccountApplicationService;
import com.lmf.finpro.application.prolabore.ProLaboreSummary.BusinessExpense;
import com.lmf.finpro.application.prolabore.ProLaboreSummary.Withdrawal;
import com.lmf.finpro.domain.model.Account;
import com.lmf.finpro.domain.model.Category;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.ProLaboreCalculationBase;
import com.lmf.finpro.domain.model.ProLaboreCalculator;
import com.lmf.finpro.domain.model.ProLaboreSettings;
import com.lmf.finpro.domain.model.ProLaboreTaxMode;
import com.lmf.finpro.domain.model.ProLaboreWithholdingMode;
import com.lmf.finpro.domain.model.SavingsGoal;
import com.lmf.finpro.domain.model.SavingsGoalCalculator;
import com.lmf.finpro.domain.model.SavingsGoalType;
import com.lmf.finpro.domain.model.TaxRateEstimator;
import com.lmf.finpro.domain.model.TaxRegime;
import com.lmf.finpro.domain.model.Transaction;
import com.lmf.finpro.domain.model.Transfer;
import com.lmf.finpro.domain.model.User;
import com.lmf.finpro.domain.port.out.AccountRepositoryPort;
import com.lmf.finpro.domain.port.out.CategoryRepositoryPort;
import com.lmf.finpro.domain.port.out.GoalContributionRepositoryPort;
import com.lmf.finpro.domain.port.out.ProLaboreSettingsRepositoryPort;
import com.lmf.finpro.domain.port.out.SavingsGoalRepositoryPort;
import com.lmf.finpro.domain.port.out.TransactionRepositoryPort;
import com.lmf.finpro.domain.port.out.TransferRepositoryPort;
import com.lmf.finpro.domain.port.out.UserRepositoryPort;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Separação PF/PJ: calcula quanto o sócio pode retirar das contas da empresa neste mês. Aqui se
 * reúnem os números (saldo, receitas e despesas PJ do mês, alíquota, retiradas); a conta em si, nas
 * duas bases configuráveis, fica em {@link ProLaboreCalculator}. O pró-labore é uma transferência
 * de conta PJ para conta PF — nada novo é persistido além da configuração.
 */
@Service
@RequiredArgsConstructor
public class ProLaboreApplicationService {

    private static final int EXPENSE_AVERAGE_MONTHS = 3;

    private final AccountRepositoryPort accountRepositoryPort;
    private final AccountApplicationService accountApplicationService;
    private final TransactionRepositoryPort transactionRepositoryPort;
    private final TransferRepositoryPort transferRepositoryPort;
    private final SavingsGoalRepositoryPort savingsGoalRepositoryPort;
    private final GoalContributionRepositoryPort goalContributionRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;
    private final ProLaboreSettingsRepositoryPort proLaboreSettingsRepositoryPort;
    private final CategoryRepositoryPort categoryRepositoryPort;
    private final Clock clock;

    public ProLaboreSummary summary(Long currentUserId) {
        LocalDate today = LocalDate.now(clock);
        YearMonth currentMonth = YearMonth.from(today);
        List<Account> accounts = accountRepositoryPort.findAllByUserId(currentUserId);
        List<Account> businessAccounts = accounts.stream().filter(Account::isBusiness).toList();
        List<Account> personalAccounts =
                accounts.stream().filter(account -> !account.isBusiness()).toList();
        ProLaboreSettings settings = getSettings(currentUserId);
        TaxRegime regime =
                userRepositoryPort.findById(currentUserId).map(User::taxRegime).orElse(null);

        Map<Long, BigDecimal> balanceByAccount =
                businessAccounts.stream()
                        .collect(
                                Collectors.toMap(
                                        Account::id,
                                        accountApplicationService::calculateCurrentBalance));
        BigDecimal businessBalance =
                balanceByAccount.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Transaction> businessTransactions =
                businessAccounts.isEmpty()
                        ? List.of()
                        : transactionRepositoryPort
                                .findAllByAccountIds(
                                        businessAccounts.stream().map(Account::id).toList())
                                .stream()
                                .filter(transaction -> transaction.transferId() == null)
                                .toList();

        List<Transaction> expenses =
                businessTransactions.stream()
                        .filter(transaction -> transaction.type() == CategoryType.EXPENSE)
                        .toList();
        // Custo do mês: o que já foi pago no mês mais o que ainda falta pagar até o fim dele,
        // inclusive as contas atrasadas.
        List<Transaction> monthExpenseTransactions =
                expenses.stream()
                        .filter(
                                transaction ->
                                        transaction.isPaid()
                                                ? inMonth(transaction, currentMonth)
                                                : !transaction
                                                        .transactionDate()
                                                        .isAfter(currentMonth.atEndOfMonth()))
                        .toList();
        BigDecimal pendingExpenses =
                sum(monthExpenseTransactions.stream().filter(transaction -> !transaction.isPaid()));
        BigDecimal monthExpenses = sum(monthExpenseTransactions.stream());
        BigDecimal monthIncome =
                sum(
                        businessTransactions.stream()
                                .filter(transaction -> transaction.type() == CategoryType.INCOME)
                                .filter(Transaction::isPaid)
                                .filter(transaction -> inMonth(transaction, currentMonth)));

        List<SavingsGoal> taxGoals =
                savingsGoalRepositoryPort.findAllByUserId(currentUserId).stream()
                        .filter(goal -> goal.type() == SavingsGoalType.TAX_RESERVE)
                        .toList();
        BigDecimal taxRate =
                settings.taxMode() == ProLaboreTaxMode.MANUAL
                        ? settings.manualTaxRate()
                        : automaticTaxRate(regime, taxGoals, monthIncome);
        BigDecimal taxReserveSaved =
                taxGoals.stream()
                        .map(
                                goal ->
                                        SavingsGoalCalculator.savedAmount(
                                                goalContributionRepositoryPort.findAllByGoalId(
                                                        goal.id())))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal averageMonthlyExpense =
                sum(businessTransactions.stream()
                                .filter(transaction -> transaction.type() == CategoryType.EXPENSE)
                                .filter(
                                        transaction ->
                                                inPreviousMonths(
                                                        transaction,
                                                        currentMonth,
                                                        EXPENSE_AVERAGE_MONTHS)))
                        .divide(
                                BigDecimal.valueOf(EXPENSE_AVERAGE_MONTHS),
                                2,
                                RoundingMode.HALF_UP);
        List<Withdrawal> withdrawals = withdrawalsOfMonth(currentUserId, accounts, currentMonth);
        BigDecimal withdrawnThisMonth =
                withdrawals.stream()
                        .map(Withdrawal::amount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        ProLaboreCalculator.Result result =
                ProLaboreCalculator.calculate(
                        new ProLaboreCalculator.Inputs(
                                settings,
                                businessBalance,
                                monthIncome,
                                monthExpenses,
                                pendingExpenses,
                                taxRate,
                                taxReserveSaved,
                                averageMonthlyExpense,
                                withdrawnThisMonth,
                                settings.withholdingAppliesTo(regime),
                                settings.effectiveEmployerInssRate(regime)));

        return new ProLaboreSummary(
                !businessAccounts.isEmpty(),
                !personalAccounts.isEmpty(),
                settings,
                result,
                businessBalance,
                monthIncome,
                monthExpenses,
                pendingExpenses,
                taxRate,
                taxReserveSaved,
                averageMonthlyExpense,
                withdrawnThisMonth,
                withdrawals,
                businessExpenses(currentUserId, monthExpenseTransactions, accounts, today),
                businessAccounts.stream()
                        .max(Comparator.comparing(account -> balanceByAccount.get(account.id())))
                        .map(Account::id)
                        .orElse(null),
                personalAccounts.stream().findFirst().map(Account::id).orElse(null));
    }

    public ProLaboreSettings getSettings(Long currentUserId) {
        return proLaboreSettingsRepositoryPort
                .findByUserId(currentUserId)
                .orElseGet(() -> ProLaboreSettings.defaults(currentUserId));
    }

    /** A alíquota manual fica guardada mesmo no modo automático, para não se perder ao alternar. */
    public ProLaboreSettings updateSettings(
            Long currentUserId,
            ProLaboreCalculationBase calculationBase,
            int cashCushionMonths,
            BigDecimal reserveRate,
            ProLaboreTaxMode taxMode,
            BigDecimal manualTaxRate,
            BigDecimal fixedAmount,
            ProLaboreWithholdingMode withholdingMode,
            BigDecimal employerInssRate) {
        return proLaboreSettingsRepositoryPort.save(
                new ProLaboreSettings(
                        currentUserId,
                        calculationBase,
                        cashCushionMonths,
                        reserveRate,
                        taxMode,
                        manualTaxRate,
                        fixedAmount,
                        withholdingMode,
                        employerInssRate));
    }

    /**
     * Modo automático: a alíquota que o usuário escolheu na caixinha do imposto tem prioridade; sem
     * ela, a de referência do regime tributário sobre a receita PJ do mês.
     */
    private BigDecimal automaticTaxRate(
            TaxRegime regime, List<SavingsGoal> taxGoals, BigDecimal monthIncome) {
        return taxGoals.stream()
                .map(SavingsGoal::incomeRate)
                .filter(rate -> rate != null && rate.signum() > 0)
                .findFirst()
                .orElseGet(
                        () ->
                                regime == null
                                        ? BigDecimal.ZERO
                                        : TaxRateEstimator.suggestRate(regime, monthIncome));
    }

    /** Pró-labore = transferências de uma conta PJ para uma conta PF no mês. */
    private List<Withdrawal> withdrawalsOfMonth(
            Long userId, List<Account> accounts, YearMonth month) {
        Map<Long, Account> accountById =
                accounts.stream().collect(Collectors.toMap(Account::id, Function.identity()));
        return transferRepositoryPort.findAllByUserId(userId).stream()
                .filter(transfer -> YearMonth.from(transfer.transferDate()).equals(month))
                .filter(transfer -> isBusiness(accountById.get(transfer.fromAccountId())))
                .filter(transfer -> isPersonal(accountById.get(transfer.toAccountId())))
                .sorted(Comparator.comparing(Transfer::transferDate).reversed())
                .map(
                        transfer ->
                                new Withdrawal(
                                        transfer.id(),
                                        transfer.transferDate(),
                                        amountInBrl(
                                                transfer,
                                                accountById.get(transfer.fromAccountId())),
                                        accountById.get(transfer.fromAccountId()).name(),
                                        accountById.get(transfer.toAccountId()).name()))
                .toList();
    }

    /** As despesas que entraram no custo do mês, com conta e categoria resolvidas. */
    private List<BusinessExpense> businessExpenses(
            Long userId, List<Transaction> transactions, List<Account> accounts, LocalDate today) {
        if (transactions.isEmpty()) {
            return List.of();
        }
        Map<Long, String> accountNames =
                accounts.stream().collect(Collectors.toMap(Account::id, Account::name));
        Map<Long, String> categoryNames =
                categoryRepositoryPort.findAllVisibleToUser(userId).stream()
                        .collect(Collectors.toMap(Category::id, Category::name));
        return transactions.stream()
                .sorted(
                        Comparator.comparing(Transaction::transactionDate)
                                .reversed()
                                .thenComparing(Transaction::baseAmount, Comparator.reverseOrder()))
                .map(
                        transaction ->
                                new BusinessExpense(
                                        transaction.id(),
                                        transaction.transactionDate(),
                                        transaction.description(),
                                        transaction.baseAmount(),
                                        transaction.isPaid(),
                                        !transaction.isPaid()
                                                && transaction.transactionDate().isBefore(today),
                                        accountNames.get(transaction.accountId()),
                                        transaction.categoryId() == null
                                                ? null
                                                : categoryNames.get(transaction.categoryId())))
                .toList();
    }

    /**
     * Retirada em reais: o lado em reais da transferência (o que caiu na conta PF, ou o que saiu da
     * PJ). Entre duas contas em moeda estrangeira, o valor em reais gravado nas transações não está
     * à mão aqui; vale o que entrou na PF.
     */
    private static BigDecimal amountInBrl(Transfer transfer, Account from) {
        if (from.currency().isBase()) {
            return transfer.amount();
        }
        return transfer.creditedAmount();
    }

    private static boolean isBusiness(Account account) {
        return account != null && account.isBusiness();
    }

    private static boolean isPersonal(Account account) {
        return account != null && !account.isBusiness();
    }

    private static boolean inMonth(Transaction transaction, YearMonth month) {
        return YearMonth.from(transaction.transactionDate()).equals(month);
    }

    private static boolean inPreviousMonths(
            Transaction transaction, YearMonth currentMonth, int months) {
        YearMonth month = YearMonth.from(transaction.transactionDate());
        return month.isBefore(currentMonth) && !month.isBefore(currentMonth.minusMonths(months));
    }

    private static BigDecimal sum(Stream<Transaction> transactions) {
        return transactions.map(Transaction::baseAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
