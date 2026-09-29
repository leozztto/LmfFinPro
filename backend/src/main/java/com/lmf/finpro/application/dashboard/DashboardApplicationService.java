package com.lmf.finpro.application.dashboard;

import com.lmf.finpro.application.exchangerate.ExchangeRateApplicationService;
import com.lmf.finpro.domain.model.Account;
import com.lmf.finpro.domain.model.AccountBalances;
import com.lmf.finpro.domain.model.AccountScope;
import com.lmf.finpro.domain.model.AccountType;
import com.lmf.finpro.domain.model.AccountValuation;
import com.lmf.finpro.domain.model.BalancePoint;
import com.lmf.finpro.domain.model.BreakdownPoint;
import com.lmf.finpro.domain.model.CashFlowProjectionPoint;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.DashboardAggregator;
import com.lmf.finpro.domain.model.DashboardOverview;
import com.lmf.finpro.domain.model.MonthlyFlowPoint;
import com.lmf.finpro.domain.model.RecurringTransaction;
import com.lmf.finpro.domain.model.Transaction;
import com.lmf.finpro.domain.port.out.AccountRepositoryPort;
import com.lmf.finpro.domain.port.out.AccountValuationRepositoryPort;
import com.lmf.finpro.domain.port.out.RecurringTransactionRepositoryPort;
import com.lmf.finpro.domain.port.out.TransactionRepositoryPort;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardApplicationService {

    private final AccountRepositoryPort accountRepositoryPort;
    private final TransactionRepositoryPort transactionRepositoryPort;
    private final RecurringTransactionRepositoryPort recurringTransactionRepositoryPort;
    private final AccountValuationRepositoryPort accountValuationRepositoryPort;
    private final Clock clock;
    private final ExchangeRateApplicationService exchangeRateApplicationService;

    public DashboardOverview getOverview(Long userId, AccountScope scope) {
        List<Account> accounts = accountsForScope(userId, scope);
        List<Transaction> transactions = ownedNonTransferTransactions(accounts);
        BigDecimal initialBalanceTotal = sumInitialBalance(accounts);
        YearMonth thisMonth = currentMonth();

        // Saldo atual só com o que já foi pago; pendentes (a receber/a pagar) formam o previsto.
        List<Transaction> paid = paidOnly(transactions);
        List<Transaction> pending =
                transactions.stream().filter(transaction -> !transaction.isPaid()).toList();

        // Contas de investimento seguem o valor de mercado informado (rendimento não resgatado).
        Function<LocalDate, BigDecimal> investmentGain = investmentGains(accounts);
        BigDecimal currentBalance =
                initialBalanceTotal
                        .add(sumByType(paid, CategoryType.INCOME))
                        .subtract(sumByType(paid, CategoryType.EXPENSE))
                        .add(investmentGain.apply(null));
        BigDecimal pendingIncome = sumByType(pending, CategoryType.INCOME);
        BigDecimal pendingExpense = sumByType(pending, CategoryType.EXPENSE);

        // Receita/despesa do mês seguem por competência (pagas e pendentes).
        List<MonthlyFlowPoint> flow = DashboardAggregator.monthlyFlow(transactions, thisMonth, 2);
        MonthlyFlowPoint previousMonth = flow.get(0);
        MonthlyFlowPoint currentMonth = flow.get(1);

        BigDecimal previousBalance =
                DashboardAggregator.balanceOverTime(paid, initialBalanceTotal, thisMonth, 2)
                        .get(0)
                        .balance()
                        .add(investmentGain.apply(thisMonth.minusMonths(1).atEndOfMonth()));

        return new DashboardOverview(
                currentBalance,
                currentMonth.income(),
                currentMonth.expense(),
                DashboardAggregator.deltaPercent(currentBalance, previousBalance),
                DashboardAggregator.deltaPercent(currentMonth.income(), previousMonth.income()),
                DashboardAggregator.deltaPercent(currentMonth.expense(), previousMonth.expense()),
                pendingIncome,
                pendingExpense,
                currentBalance.add(pendingIncome).subtract(pendingExpense));
    }

    public List<MonthlyFlowPoint> getMonthlyFlow(Long userId, int monthsCount, AccountScope scope) {
        return DashboardAggregator.monthlyFlow(
                ownedNonTransferTransactions(userId, scope), currentMonth(), monthsCount);
    }

    public List<BalancePoint> getBalanceEvolution(
            Long userId, int monthsCount, AccountScope scope) {
        // Histórico real: só pagas, para o último ponto bater com o saldo atual do overview.
        List<Account> accounts = accountsForScope(userId, scope);
        Function<LocalDate, BigDecimal> investmentGain = investmentGains(accounts);
        return DashboardAggregator.balanceOverTime(
                        paidOnly(ownedNonTransferTransactions(accounts)),
                        sumInitialBalance(accounts),
                        currentMonth(),
                        monthsCount)
                .stream()
                .map(
                        point ->
                                new BalancePoint(
                                        point.month(),
                                        point.balance()
                                                .add(
                                                        investmentGain.apply(
                                                                point.month().atEndOfMonth()))))
                .toList();
    }

    /**
     * Anexa a projeção ao saldo real do fim do mês atual (exclui transações com data futura),
     * somando as ocorrências dos lançamentos recorrentes do usuário.
     */
    public List<CashFlowProjectionPoint> getCashFlowProjection(
            Long userId, int monthsAhead, AccountScope scope) {
        List<Account> accounts = accountsForScope(userId, scope);
        List<Transaction> transactions = ownedNonTransferTransactions(accounts);
        BigDecimal anchorBalance =
                DashboardAggregator.balanceOverTime(
                                transactions, sumInitialBalance(accounts), currentMonth(), 1)
                        .get(0)
                        .balance()
                        .add(investmentGains(accounts).apply(currentMonth().atEndOfMonth()));
        List<Long> accountIds = accounts.stream().map(Account::id).toList();
        List<RecurringTransaction> recurrences =
                recurringTransactionRepositoryPort.findAllByUserId(userId).stream()
                        .filter(recurrence -> accountIds.contains(recurrence.accountId()))
                        .toList();
        return DashboardAggregator.cashFlowProjection(
                transactions,
                anchorBalance,
                currentMonth(),
                monthsAhead,
                recurrencesInBrl(accounts, recurrences));
    }

    private List<RecurringTransaction> recurrencesInBrl(
            List<Account> accounts, List<RecurringTransaction> recurrences) {
        return accounts.stream().allMatch(account -> account.currency().isBase())
                ? recurrences
                : exchangeRateApplicationService.recurrencesInBrl(accounts, recurrences);
    }

    public List<BreakdownPoint> getCategoryBreakdown(
            Long userId, CategoryType type, YearMonth month, AccountScope scope) {
        return DashboardAggregator.categoryBreakdown(
                ownedNonTransferTransactions(userId, scope), type, month);
    }

    public List<BreakdownPoint> getClientBreakdown(
            Long userId, YearMonth month, AccountScope scope) {
        return DashboardAggregator.clientBreakdown(
                ownedNonTransferTransactions(userId, scope), month);
    }

    private List<Transaction> ownedNonTransferTransactions(Long userId, AccountScope scope) {
        return ownedNonTransferTransactions(accountsForScope(userId, scope));
    }

    private List<Account> accountsForScope(Long userId, AccountScope scope) {
        List<Account> accounts = accountRepositoryPort.findAllByUserId(userId);
        return scope == null
                ? accounts
                : accounts.stream().filter(account -> account.scope() == scope).toList();
    }

    private List<Transaction> ownedNonTransferTransactions(List<Account> accounts) {
        List<Long> accountIds = accounts.stream().map(Account::id).toList();
        return transactionRepositoryPort.findAllByAccountIds(accountIds).stream()
                .filter(transaction -> transaction.transferId() == null)
                .toList();
    }

    /**
     * Rendimento ainda não realizado das contas de investimento com valor de mercado informado,
     * como função da data: o saldo consolidado soma esse ajuste para bater com a tela de Contas.
     * Carrega os dados uma vez só; sem valorizações, é sempre zero (e não consulta transações).
     */
    private Function<LocalDate, BigDecimal> investmentGains(List<Account> accounts) {
        List<Account> investments =
                accounts.stream()
                        .filter(account -> account.type() == AccountType.INVESTMENT)
                        .toList();
        List<Long> ids = investments.stream().map(Account::id).toList();
        List<AccountValuation> valuations =
                ids.isEmpty() ? List.of() : accountValuationRepositoryPort.findAllByAccountIds(ids);
        if (valuations.isEmpty()) {
            return asOf -> BigDecimal.ZERO;
        }
        Map<Long, List<Transaction>> transactionsByAccount =
                transactionRepositoryPort.findAllByAccountIds(ids).stream()
                        .collect(Collectors.groupingBy(Transaction::accountId));
        return asOf ->
                investments.stream()
                        .map(
                                account ->
                                        toBrl(
                                                account,
                                                AccountBalances.valuationGain(
                                                        account,
                                                        transactionsByAccount.getOrDefault(
                                                                account.id(), List.of()),
                                                        valuations,
                                                        asOf),
                                                asOf == null ? LocalDate.now(clock) : asOf))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Valor da conta em reais na cotação do dia. As transações já trazem o valor em reais gravado
     * ({@link Transaction#baseAmount()}); isto vale para o que é da conta em si (saldo inicial,
     * rendimento informado).
     */
    private BigDecimal toBrl(Account account, BigDecimal amount, LocalDate date) {
        if (account.currency().isBase() || amount.signum() == 0) {
            return amount;
        }
        return exchangeRateApplicationService.toBrl(account.currency(), amount, date);
    }

    private YearMonth currentMonth() {
        return YearMonth.now(clock);
    }

    private List<Transaction> paidOnly(List<Transaction> transactions) {
        return transactions.stream().filter(Transaction::isPaid).toList();
    }

    /** Saldos iniciais em reais: os de conta em outra moeda, pela cotação do dia da criação. */
    private BigDecimal sumInitialBalance(List<Account> accounts) {
        return accounts.stream()
                .map(
                        account ->
                                toBrl(
                                        account,
                                        account.initialBalance(),
                                        account.createdAt() == null
                                                ? LocalDate.now(clock)
                                                : account.createdAt().toLocalDate()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumByType(List<Transaction> transactions, CategoryType type) {
        return transactions.stream()
                .filter(transaction -> transaction.type() == type)
                .map(Transaction::baseAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
