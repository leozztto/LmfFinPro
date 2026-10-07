package com.lmf.finpro.application.dashboard;

import com.lmf.finpro.application.exchangerate.ExchangeRateApplicationService;
import com.lmf.finpro.application.support.TransferFlow;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardApplicationService {

    private final AccountRepositoryPort accountRepositoryPort;
    private final TransactionRepositoryPort transactionRepositoryPort;
    private final RecurringTransactionRepositoryPort recurringTransactionRepositoryPort;
    private final AccountValuationRepositoryPort accountValuationRepositoryPort;
    private final Clock clock;
    private final ExchangeRateApplicationService exchangeRateApplicationService;
    private final TransferFlow transferFlow;

    public DashboardOverview getOverview(Long householdId, AccountScope scope) {
        log.debug("Montando visão geral do dashboard do usuário={} escopo={}", householdId, scope);
        List<Account> accounts = accountsForScope(householdId, scope);
        List<Transaction> allTransactions = ownedTransactions(accounts);
        List<Transaction> transactions = withoutInternalTransfers(allTransactions);
        BigDecimal initialBalanceTotal = sumInitialBalance(accounts);
        YearMonth thisMonth = currentMonth();

        // Saldo atual só com o que já foi pago; pendentes (a receber/a pagar) formam o previsto. O
        // saldo conta as pernas de transferência: entre contas do mesmo espaço elas se anulam, mas
        // uma transferência com uma conta de outro espaço (pessoal x grupo) movimenta o saldo
        // daqui.
        List<Transaction> paid = paidOnly(allTransactions);
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

    public List<MonthlyFlowPoint> getMonthlyFlow(
            Long householdId, int monthsCount, AccountScope scope) {
        log.debug("Calculando fluxo mensal do usuário={} meses={}", householdId, monthsCount);
        return DashboardAggregator.monthlyFlow(
                ownedNonTransferTransactions(householdId, scope), currentMonth(), monthsCount);
    }

    public List<BalancePoint> getBalanceEvolution(
            Long householdId, int monthsCount, AccountScope scope) {
        log.debug("Calculando evolução do saldo do usuário={} meses={}", householdId, monthsCount);
        // Histórico real: só pagas, para o último ponto bater com o saldo atual do overview.
        List<Account> accounts = accountsForScope(householdId, scope);
        Function<LocalDate, BigDecimal> investmentGain = investmentGains(accounts);
        List<Transaction> transactions = ownedTransactions(accounts);
        // O saldo inicial de uma conta só entra a partir do mês em que ela passou a existir.
        return DashboardAggregator.balanceOverTime(
                        paidOnly(transactions), BigDecimal.ZERO, currentMonth(), monthsCount)
                .stream()
                .map(
                        point ->
                                new BalancePoint(
                                        point.month(),
                                        point.balance()
                                                .add(
                                                        initialBalanceAsOf(
                                                                accounts,
                                                                transactions,
                                                                point.month()))
                                                .add(
                                                        investmentGain.apply(
                                                                point.month().atEndOfMonth()))))
                .toList();
    }

    /**
     * Soma o saldo inicial (em reais) das contas que já existiam no mês: a conta existe desde o mês
     * de criação ou desde a transação mais antiga, o que vier primeiro (importações podem trazer
     * lançamentos anteriores à criação).
     */
    private BigDecimal initialBalanceAsOf(
            List<Account> accounts, List<Transaction> transactions, YearMonth month) {
        Map<Long, YearMonth> firstTransactionMonth =
                transactions.stream()
                        .collect(
                                Collectors.toMap(
                                        Transaction::accountId,
                                        transaction ->
                                                YearMonth.from(transaction.transactionDate()),
                                        (a, b) -> a.isBefore(b) ? a : b));
        List<Account> existing =
                accounts.stream()
                        .filter(
                                account -> {
                                    YearMonth start =
                                            account.createdAt() == null
                                                    ? null
                                                    : YearMonth.from(account.createdAt());
                                    YearMonth firstTx = firstTransactionMonth.get(account.id());
                                    if (start == null
                                            || (firstTx != null && firstTx.isBefore(start))) {
                                        start = firstTx;
                                    }
                                    return start == null || !start.isAfter(month);
                                })
                        .toList();
        return sumInitialBalance(existing);
    }

    /**
     * Anexa a projeção ao saldo real do fim do mês atual (exclui transações com data futura),
     * somando as ocorrências dos lançamentos recorrentes do usuário.
     */
    public List<CashFlowProjectionPoint> getCashFlowProjection(
            Long householdId, int monthsAhead, AccountScope scope) {
        log.debug("Projetando fluxo de caixa do usuário={} meses={}", householdId, monthsAhead);
        List<Account> accounts = accountsForScope(householdId, scope);
        List<Transaction> allTransactions = ownedTransactions(accounts);
        List<Transaction> transactions = withoutInternalTransfers(allTransactions);
        BigDecimal anchorBalance =
                DashboardAggregator.balanceOverTime(
                                allTransactions, sumInitialBalance(accounts), currentMonth(), 1)
                        .get(0)
                        .balance()
                        .add(investmentGains(accounts).apply(currentMonth().atEndOfMonth()));
        List<Long> accountIds = accounts.stream().map(Account::id).toList();
        List<RecurringTransaction> recurrences =
                recurringTransactionRepositoryPort.findAllByHouseholdId(householdId).stream()
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
            Long householdId, CategoryType type, YearMonth month, AccountScope scope) {
        log.debug(
                "Calculando distribuição por categoria do usuário={} tipo={} mês={}",
                householdId,
                type,
                month);
        return DashboardAggregator.categoryBreakdown(
                ownedNonTransferTransactions(householdId, scope), type, month);
    }

    public List<BreakdownPoint> getClientBreakdown(
            Long householdId, YearMonth month, AccountScope scope) {
        log.debug("Calculando distribuição por cliente do usuário={} mês={}", householdId, month);
        return DashboardAggregator.clientBreakdown(
                ownedNonTransferTransactions(householdId, scope), month);
    }

    private List<Transaction> ownedNonTransferTransactions(Long householdId, AccountScope scope) {
        return ownedNonTransferTransactions(accountsForScope(householdId, scope));
    }

    private List<Account> accountsForScope(Long householdId, AccountScope scope) {
        List<Account> accounts = accountRepositoryPort.findAllByHouseholdId(householdId);
        return scope == null
                ? accounts
                : accounts.stream().filter(account -> account.scope() == scope).toList();
    }

    private List<Transaction> ownedNonTransferTransactions(List<Account> accounts) {
        return withoutInternalTransfers(ownedTransactions(accounts));
    }

    /** Todas as transações das contas, inclusive as pernas de transferência. */
    private List<Transaction> ownedTransactions(List<Account> accounts) {
        List<Long> accountIds = accounts.stream().map(Account::id).toList();
        return transactionRepositoryPort.findAllByAccountIds(accountIds);
    }

    /**
     * Receita e despesa de verdade: transferência entre contas do mesmo espaço só move dinheiro e
     * fica de fora; a que cruza para outro espaço (pessoal x grupo) conta, pois o dinheiro entrou
     * ou saiu daqui.
     */
    private List<Transaction> withoutInternalTransfers(List<Transaction> transactions) {
        return transferFlow.withoutInternalTransfers(transactions);
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
