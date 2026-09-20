package com.lmf.finpro.application.dashboard;

import com.lmf.finpro.domain.model.Account;
import com.lmf.finpro.domain.model.BalancePoint;
import com.lmf.finpro.domain.model.BreakdownPoint;
import com.lmf.finpro.domain.model.CashFlowProjectionPoint;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.DashboardAggregator;
import com.lmf.finpro.domain.model.DashboardOverview;
import com.lmf.finpro.domain.model.MonthlyFlowPoint;
import com.lmf.finpro.domain.model.Transaction;
import com.lmf.finpro.domain.port.out.AccountRepositoryPort;
import com.lmf.finpro.domain.port.out.TransactionRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardApplicationService {

    private final AccountRepositoryPort accountRepositoryPort;
    private final TransactionRepositoryPort transactionRepositoryPort;

    public DashboardOverview getOverview(Long userId) {
        List<Account> accounts = accountRepositoryPort.findAllByUserId(userId);
        List<Transaction> transactions = ownedNonTransferTransactions(accounts);
        BigDecimal initialBalanceTotal = sumInitialBalance(accounts);

        BigDecimal totalIncome = sumByType(transactions, CategoryType.INCOME);
        BigDecimal totalExpense = sumByType(transactions, CategoryType.EXPENSE);
        BigDecimal currentBalance = initialBalanceTotal.add(totalIncome).subtract(totalExpense);

        List<MonthlyFlowPoint> flow = DashboardAggregator.monthlyFlow(transactions, 2);
        MonthlyFlowPoint previousMonth = flow.get(0);
        MonthlyFlowPoint currentMonth = flow.get(1);

        BigDecimal previousBalance = DashboardAggregator.balanceOverTime(transactions, initialBalanceTotal, 2).get(0).balance();

        return new DashboardOverview(
            currentBalance,
            currentMonth.income(),
            currentMonth.expense(),
            DashboardAggregator.deltaPercent(currentBalance, previousBalance),
            DashboardAggregator.deltaPercent(currentMonth.income(), previousMonth.income()),
            DashboardAggregator.deltaPercent(currentMonth.expense(), previousMonth.expense())
        );
    }

    public List<MonthlyFlowPoint> getMonthlyFlow(Long userId, int monthsCount) {
        return DashboardAggregator.monthlyFlow(ownedNonTransferTransactions(userId), monthsCount);
    }

    public List<BalancePoint> getBalanceEvolution(Long userId, int monthsCount) {
        return DashboardAggregator.balanceOverTime(ownedNonTransferTransactions(userId), sumInitialBalance(userId), monthsCount);
    }

    /** Anexa a projeção ao saldo real do fim do mês atual (exclui transações com data futura). */
    public List<CashFlowProjectionPoint> getCashFlowProjection(Long userId, int monthsAhead) {
        List<Transaction> transactions = ownedNonTransferTransactions(userId);
        BigDecimal anchorBalance = DashboardAggregator.balanceOverTime(transactions, sumInitialBalance(userId), 1).get(0).balance();
        return DashboardAggregator.cashFlowProjection(transactions, anchorBalance, monthsAhead);
    }

    public List<BreakdownPoint> getCategoryBreakdown(Long userId, CategoryType type, YearMonth month) {
        return DashboardAggregator.categoryBreakdown(ownedNonTransferTransactions(userId), type, month);
    }

    public List<BreakdownPoint> getClientBreakdown(Long userId, YearMonth month) {
        return DashboardAggregator.clientBreakdown(ownedNonTransferTransactions(userId), month);
    }

    private List<Transaction> ownedNonTransferTransactions(Long userId) {
        return ownedNonTransferTransactions(accountRepositoryPort.findAllByUserId(userId));
    }

    private List<Transaction> ownedNonTransferTransactions(List<Account> accounts) {
        List<Long> accountIds = accounts.stream().map(Account::id).toList();
        return transactionRepositoryPort.findAllByAccountIds(accountIds).stream()
            .filter(transaction -> transaction.transferId() == null)
            .toList();
    }

    private BigDecimal sumInitialBalance(Long userId) {
        return sumInitialBalance(accountRepositoryPort.findAllByUserId(userId));
    }

    private BigDecimal sumInitialBalance(List<Account> accounts) {
        return accounts.stream().map(Account::initialBalance).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumByType(List<Transaction> transactions, CategoryType type) {
        return transactions.stream()
            .filter(transaction -> transaction.type() == type)
            .map(Transaction::amount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
