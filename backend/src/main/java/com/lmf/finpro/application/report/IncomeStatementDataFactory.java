package com.lmf.finpro.application.report;

import com.lmf.finpro.domain.model.Account;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.IncomeStatementData;
import com.lmf.finpro.domain.model.ReportGranularity;
import com.lmf.finpro.domain.model.Transaction;
import com.lmf.finpro.domain.model.User;
import com.lmf.finpro.domain.port.out.AccountRepositoryPort;
import com.lmf.finpro.domain.port.out.TransactionRepositoryPort;
import java.math.BigDecimal;
import java.time.Month;
import java.time.Year;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Monta os dados do resultado do período (DRE), por mês, trimestre ou ano. */
@Component
@RequiredArgsConstructor
class IncomeStatementDataFactory {

    private static final Locale PT_BR = Locale.of("pt", "BR");

    private final ReportLookups lookups;
    private final AccountRepositoryPort accountRepositoryPort;
    private final TransactionRepositoryPort transactionRepositoryPort;

    /**
     * Receita, despesa e resultado consolidados por período (mês, trimestre ou o ano inteiro,
     * conforme {@code granularity}) dentro do ano informado, em todas as contas do usuário —
     * transferências entre contas próprias são excluídas, igual ao Dashboard.
     */
    IncomeStatementData build(Long currentUserId, Year year, ReportGranularity granularity) {
        User issuer = lookups.findUserOrThrow(currentUserId);

        List<Long> accountIds =
                accountRepositoryPort.findAllByUserId(currentUserId).stream()
                        .map(Account::id)
                        .toList();
        List<Transaction> transactionsInYear =
                transactionRepositoryPort.findAllByAccountIds(accountIds).stream()
                        .filter(transaction -> transaction.transferId() == null)
                        .filter(
                                transaction ->
                                        transaction.transactionDate().getYear() == year.getValue())
                        .toList();

        List<IncomeStatementData.PeriodResult> periods =
                switch (granularity) {
                    case MONTHLY -> monthlyPeriods(transactionsInYear);
                    case QUARTERLY -> quarterlyPeriods(transactionsInYear);
                    case YEARLY -> yearlyPeriod(transactionsInYear, year);
                };

        BigDecimal totalIncome =
                periods.stream()
                        .map(IncomeStatementData.PeriodResult::income)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalExpense =
                periods.stream()
                        .map(IncomeStatementData.PeriodResult::expense)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalResult = totalIncome.subtract(totalExpense);

        return new IncomeStatementData(
                issuer, year, granularity, periods, totalIncome, totalExpense, totalResult);
    }

    private List<IncomeStatementData.PeriodResult> monthlyPeriods(List<Transaction> transactions) {
        Map<Month, BigDecimal> incomeByMonth = new EnumMap<>(Month.class);
        Map<Month, BigDecimal> expenseByMonth = new EnumMap<>(Month.class);
        for (Month month : Month.values()) {
            incomeByMonth.put(month, BigDecimal.ZERO);
            expenseByMonth.put(month, BigDecimal.ZERO);
        }
        for (Transaction transaction : transactions) {
            Month month = transaction.transactionDate().getMonth();
            if (transaction.type() == CategoryType.INCOME) {
                incomeByMonth.merge(month, transaction.baseAmount(), BigDecimal::add);
            } else {
                expenseByMonth.merge(month, transaction.baseAmount(), BigDecimal::add);
            }
        }
        return Arrays.stream(Month.values())
                .map(
                        month ->
                                periodResult(
                                        capitalizeMonth(month),
                                        incomeByMonth.get(month),
                                        expenseByMonth.get(month)))
                .toList();
    }

    private List<IncomeStatementData.PeriodResult> quarterlyPeriods(
            List<Transaction> transactions) {
        List<IncomeStatementData.PeriodResult> periods = new ArrayList<>();
        for (int quarter = 1; quarter <= 4; quarter++) {
            int startMonth = (quarter - 1) * 3 + 1;
            int endMonthExclusive = startMonth + 3;
            BigDecimal income = BigDecimal.ZERO;
            BigDecimal expense = BigDecimal.ZERO;
            for (Transaction transaction : transactions) {
                int monthValue = transaction.transactionDate().getMonthValue();
                if (monthValue < startMonth || monthValue >= endMonthExclusive) {
                    continue;
                }
                if (transaction.type() == CategoryType.INCOME) {
                    income = income.add(transaction.baseAmount());
                } else {
                    expense = expense.add(transaction.baseAmount());
                }
            }
            periods.add(periodResult(quarter + "º trimestre", income, expense));
        }
        return periods;
    }

    private List<IncomeStatementData.PeriodResult> yearlyPeriod(
            List<Transaction> transactions, Year year) {
        BigDecimal income =
                transactions.stream()
                        .filter(transaction -> transaction.type() == CategoryType.INCOME)
                        .map(Transaction::baseAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal expense =
                transactions.stream()
                        .filter(transaction -> transaction.type() == CategoryType.EXPENSE)
                        .map(Transaction::baseAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        return List.of(periodResult(year.toString(), income, expense));
    }

    private IncomeStatementData.PeriodResult periodResult(
            String label, BigDecimal income, BigDecimal expense) {
        return new IncomeStatementData.PeriodResult(
                label, income, expense, income.subtract(expense));
    }

    private String capitalizeMonth(Month month) {
        String name = month.getDisplayName(TextStyle.FULL, PT_BR);
        return name.substring(0, 1).toUpperCase(PT_BR) + name.substring(1);
    }
}
