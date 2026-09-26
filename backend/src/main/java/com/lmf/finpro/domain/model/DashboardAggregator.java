package com.lmf.finpro.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Cálculos puros do dashboard (sem dependência de repositório) — a partir de uma lista de
 * transações já filtrada (do usuário, sem transferências), devolve os pontos prontos para cada
 * gráfico. Espelha fielmente o que antes vivia em {@code frontend/src/features/dashboard/utils.ts}.
 */
public final class DashboardAggregator {

    private DashboardAggregator() {}

    /** Últimos {@code count} meses (do mais antigo ao atual, inclusive). */
    public static List<YearMonth> lastMonths(int count) {
        YearMonth current = YearMonth.now();
        List<YearMonth> result = new ArrayList<>();
        for (int i = count - 1; i >= 0; i--) {
            result.add(current.minusMonths(i));
        }
        return result;
    }

    /** Próximos {@code count} meses após o atual (do mais próximo ao mais distante). */
    public static List<YearMonth> nextMonths(int count) {
        YearMonth current = YearMonth.now();
        List<YearMonth> result = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            result.add(current.plusMonths(i));
        }
        return result;
    }

    /** Receita e despesa somadas por mês, para os últimos {@code monthsCount} meses. */
    public static List<MonthlyFlowPoint> monthlyFlow(
            List<Transaction> transactions, int monthsCount) {
        List<YearMonth> months = lastMonths(monthsCount);
        Map<YearMonth, BigDecimal> income = new LinkedHashMap<>();
        Map<YearMonth, BigDecimal> expense = new LinkedHashMap<>();
        for (YearMonth month : months) {
            income.put(month, BigDecimal.ZERO);
            expense.put(month, BigDecimal.ZERO);
        }

        for (Transaction transaction : transactions) {
            YearMonth month = YearMonth.from(transaction.transactionDate());
            if (!income.containsKey(month)) {
                continue;
            }
            if (transaction.type() == CategoryType.INCOME) {
                income.merge(month, transaction.amount(), BigDecimal::add);
            } else {
                expense.merge(month, transaction.amount(), BigDecimal::add);
            }
        }

        return months.stream()
                .map(month -> new MonthlyFlowPoint(month, income.get(month), expense.get(month)))
                .toList();
    }

    /** Saldo consolidado ao final de cada um dos últimos {@code monthsCount} meses. */
    public static List<BalancePoint> balanceOverTime(
            List<Transaction> transactions, BigDecimal initialBalanceTotal, int monthsCount) {
        List<YearMonth> months = lastMonths(monthsCount);
        return months.stream()
                .map(
                        month ->
                                new BalancePoint(
                                        month,
                                        balanceAsOf(transactions, initialBalanceTotal, month)))
                .toList();
    }

    private static BigDecimal balanceAsOf(
            List<Transaction> transactions, BigDecimal initialBalanceTotal, YearMonth month) {
        BigDecimal balance = initialBalanceTotal;
        for (Transaction transaction : transactions) {
            if (YearMonth.from(transaction.transactionDate()).isAfter(month)) {
                continue;
            }
            balance =
                    balance.add(
                            transaction.type() == CategoryType.INCOME
                                    ? transaction.amount()
                                    : transaction.amount().negate());
        }
        return balance;
    }

    /** Projeção sem lançamentos recorrentes — ver a sobrecarga completa. */
    public static List<CashFlowProjectionPoint> cashFlowProjection(
            List<Transaction> transactions, BigDecimal currentBalance, int monthsAhead) {
        return cashFlowProjection(transactions, currentBalance, monthsAhead, List.of());
    }

    /**
     * Projeção para os próximos {@code monthsAhead} meses a partir de {@code currentBalance} (saldo
     * ao final do mês atual). Cada mês soma duas partes:
     *
     * <ul>
     *   <li><b>variável</b>: o líquido real das transações avulsas já cadastradas para o mês, ou,
     *       se não houver nenhuma, a média móvel do líquido dos últimos 3 meses;
     *   <li><b>recorrente</b>: as ocorrências ainda não lançadas das recorrências ativas que caem
     *       no mês, com as datas exatas de cada uma.
     * </ul>
     *
     * <p>Para não contar a mesma coisa duas vezes, as transações já lançadas por uma recorrência
     * ativa ficam fora da média (ela já entra pela parte recorrente), e as ocorrências que ainda
     * vão cair no mês atual somam ao saldo de partida — o {@code currentBalance} só enxerga o que
     * já virou transação.
     */
    public static List<CashFlowProjectionPoint> cashFlowProjection(
            List<Transaction> transactions,
            BigDecimal currentBalance,
            int monthsAhead,
            List<RecurringTransaction> recurrences) {
        List<RecurringTransaction> activeRecurrences =
                recurrences.stream().filter(RecurringTransaction::active).toList();
        Set<Long> activeRecurrenceIds =
                activeRecurrences.stream()
                        .map(RecurringTransaction::id)
                        .collect(Collectors.toSet());
        List<Transaction> variableTransactions =
                transactions.stream()
                        .filter(
                                transaction ->
                                        transaction.recurringTransactionId() == null
                                                || !activeRecurrenceIds.contains(
                                                        transaction.recurringTransactionId()))
                        .toList();

        List<MonthlyFlowPoint> recentFlow = monthlyFlow(variableTransactions, 3);
        BigDecimal totalNet = BigDecimal.ZERO;
        for (MonthlyFlowPoint point : recentFlow) {
            totalNet = totalNet.add(point.income()).subtract(point.expense());
        }
        BigDecimal averageNet =
                totalNet.divide(BigDecimal.valueOf(recentFlow.size()), 10, RoundingMode.HALF_UP);

        YearMonth currentMonth = YearMonth.now();
        Map<YearMonth, BigDecimal> recurringNetByMonth =
                pendingRecurringNetByMonth(
                        activeRecurrences, currentMonth.plusMonths(monthsAhead).atEndOfMonth());

        // Pendentes até o fim do mês atual (inclusive as atrasadas, se o agendamento ainda não
        // rodou) ajustam o saldo de partida.
        BigDecimal balance = currentBalance;
        for (Map.Entry<YearMonth, BigDecimal> entry : recurringNetByMonth.entrySet()) {
            if (!entry.getKey().isAfter(currentMonth)) {
                balance = balance.add(entry.getValue());
            }
        }

        List<CashFlowProjectionPoint> result = new ArrayList<>();
        for (YearMonth month : nextMonths(monthsAhead)) {
            List<Transaction> monthTransactions =
                    variableTransactions.stream()
                            .filter(
                                    transaction ->
                                            YearMonth.from(transaction.transactionDate())
                                                    .equals(month))
                            .toList();

            BigDecimal net = averageNet;
            if (!monthTransactions.isEmpty()) {
                net = BigDecimal.ZERO;
                for (Transaction transaction : monthTransactions) {
                    net = net.add(signedAmount(transaction.type(), transaction.amount()));
                }
            }
            net = net.add(recurringNetByMonth.getOrDefault(month, BigDecimal.ZERO));

            balance = balance.add(net);
            result.add(new CashFlowProjectionPoint(month, balance, true));
        }
        return result;
    }

    /**
     * Líquido, por mês, das ocorrências recorrentes ainda não lançadas até {@code lastDate}
     * (inclusive). Ocorrências já lançadas não entram — elas já são transações.
     */
    private static Map<YearMonth, BigDecimal> pendingRecurringNetByMonth(
            List<RecurringTransaction> activeRecurrences, LocalDate lastDate) {
        Map<YearMonth, BigDecimal> netByMonth = new HashMap<>();
        for (RecurringTransaction recurrence : activeRecurrences) {
            BigDecimal amount = signedAmount(recurrence.type(), recurrence.amount());
            for (LocalDate date : recurrence.dueOccurrenceDates(lastDate)) {
                netByMonth.merge(YearMonth.from(date), amount, BigDecimal::add);
            }
        }
        return netByMonth;
    }

    private static BigDecimal signedAmount(CategoryType type, BigDecimal amount) {
        return type == CategoryType.INCOME ? amount : amount.negate();
    }

    /**
     * Transações do tipo/mês informados, somadas por categoria e ordenadas da maior para a menor.
     */
    public static List<BreakdownPoint> categoryBreakdown(
            List<Transaction> transactions, CategoryType type, YearMonth month) {
        return breakdown(
                transactions,
                month,
                transaction -> transaction.type() == type,
                Transaction::categoryId);
    }

    /** Receita do mês informado, somada por cliente e ordenada da maior para a menor. */
    public static List<BreakdownPoint> clientBreakdown(
            List<Transaction> transactions, YearMonth month) {
        return breakdown(
                transactions,
                month,
                transaction -> transaction.type() == CategoryType.INCOME,
                Transaction::clientId);
    }

    private interface EntityIdExtractor {
        Long apply(Transaction transaction);
    }

    private static List<BreakdownPoint> breakdown(
            List<Transaction> transactions,
            YearMonth month,
            java.util.function.Predicate<Transaction> typeFilter,
            EntityIdExtractor entityIdExtractor) {
        Map<Long, BigDecimal> totals = new LinkedHashMap<>();
        for (Transaction transaction : transactions) {
            if (!typeFilter.test(transaction)) {
                continue;
            }
            if (!YearMonth.from(transaction.transactionDate()).equals(month)) {
                continue;
            }
            totals.merge(
                    entityIdExtractor.apply(transaction), transaction.amount(), BigDecimal::add);
        }

        return totals.entrySet().stream()
                .map(entry -> new BreakdownPoint(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(BreakdownPoint::value).reversed())
                .toList();
    }

    /** Variação percentual entre dois períodos; {@code null} quando o período anterior é zero. */
    public static Double deltaPercent(BigDecimal current, BigDecimal previous) {
        if (previous.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return current.subtract(previous)
                .divide(previous.abs(), 10, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }
}
