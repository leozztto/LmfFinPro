package com.lmf.finpro.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Histórico, ranking e concentração de receita por cliente num conjunto de meses — puro, sem acesso
 * a repositório. Recebe as transações já filtradas (sem transferências, só do período e da situação
 * desejada).
 */
public final class ClientAnalytics {

    private static final int SHARE_SCALE = 4;

    private ClientAnalytics() {}

    /**
     * @param unassignedIncome receitas sem cliente vinculado — entram no total (a concentração é
     *     sobre toda a receita), mas não no ranking
     * @param averageTicket receita com cliente ÷ número de recebimentos com cliente
     * @param topClientShare fração da receita total que vem do maior cliente
     * @param topThreeShare fração da receita total que vem dos três maiores
     */
    public record Report(
            List<YearMonth> months,
            BigDecimal totalIncome,
            BigDecimal unassignedIncome,
            int activeClients,
            BigDecimal averageTicket,
            BigDecimal topClientShare,
            BigDecimal topThreeShare,
            ConcentrationRisk risk,
            List<ClientSummary> ranking) {}

    /**
     * @param share fração da receita total do período
     * @param activeMonths meses do período com alguma receita do cliente
     * @param monthly receita e despesa do cliente em cada mês do período, na ordem dos meses
     */
    public record ClientSummary(
            Long clientId,
            BigDecimal income,
            BigDecimal expense,
            BigDecimal net,
            int incomeCount,
            BigDecimal averageTicket,
            BigDecimal share,
            int activeMonths,
            LocalDate lastIncomeDate,
            List<MonthlyValues> monthly) {}

    public record MonthlyValues(YearMonth month, BigDecimal income, BigDecimal expense) {}

    public static Report analyze(List<Transaction> transactions, List<YearMonth> months) {
        Set<YearMonth> period = new HashSet<>(months);
        Map<Long, Accumulator> byClient = new LinkedHashMap<>();
        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal unassignedIncome = BigDecimal.ZERO;

        for (Transaction transaction : transactions) {
            YearMonth month = YearMonth.from(transaction.transactionDate());
            if (!period.contains(month)) {
                continue;
            }
            boolean isIncome = transaction.type() == CategoryType.INCOME;
            if (isIncome) {
                totalIncome = totalIncome.add(transaction.baseAmount());
            }
            if (transaction.clientId() == null) {
                if (isIncome) {
                    unassignedIncome = unassignedIncome.add(transaction.baseAmount());
                }
                continue;
            }
            byClient.computeIfAbsent(transaction.clientId(), id -> new Accumulator())
                    .add(transaction, month, isIncome);
        }

        BigDecimal total = totalIncome;
        List<ClientSummary> ranking =
                byClient.entrySet().stream()
                        .map(entry -> entry.getValue().summarize(entry.getKey(), months, total))
                        .sorted(
                                Comparator.comparing(ClientSummary::income)
                                        .thenComparing(ClientSummary::expense)
                                        .reversed())
                        .toList();

        BigDecimal assignedIncome = totalIncome.subtract(unassignedIncome);
        int assignedCount = ranking.stream().mapToInt(ClientSummary::incomeCount).sum();
        BigDecimal topShare = ranking.isEmpty() ? BigDecimal.ZERO : ranking.get(0).share();
        // Somado a partir dos valores, e não das participações já arredondadas.
        BigDecimal topThreeShare =
                share(
                        ranking.stream()
                                .limit(3)
                                .map(ClientSummary::income)
                                .reduce(BigDecimal.ZERO, BigDecimal::add),
                        totalIncome);
        boolean hasIncome = totalIncome.signum() > 0;

        return new Report(
                months,
                totalIncome,
                unassignedIncome,
                (int) ranking.stream().filter(summary -> summary.incomeCount() > 0).count(),
                average(assignedIncome, assignedCount),
                topShare,
                topThreeShare,
                ConcentrationRisk.of(topShare, hasIncome),
                ranking);
    }

    private static BigDecimal share(BigDecimal part, BigDecimal total) {
        return total.signum() == 0
                ? BigDecimal.ZERO
                : part.divide(total, SHARE_SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal average(BigDecimal amount, int count) {
        return count == 0
                ? BigDecimal.ZERO
                : amount.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
    }

    private static final class Accumulator {
        private BigDecimal income = BigDecimal.ZERO;
        private BigDecimal expense = BigDecimal.ZERO;
        private int incomeCount;
        private LocalDate lastIncomeDate;
        private final Map<YearMonth, BigDecimal[]> byMonth = new LinkedHashMap<>();

        void add(Transaction transaction, YearMonth month, boolean isIncome) {
            BigDecimal[] values =
                    byMonth.computeIfAbsent(
                            month, key -> new BigDecimal[] {BigDecimal.ZERO, BigDecimal.ZERO});
            if (isIncome) {
                income = income.add(transaction.baseAmount());
                incomeCount++;
                values[0] = values[0].add(transaction.baseAmount());
                if (lastIncomeDate == null
                        || transaction.transactionDate().isAfter(lastIncomeDate)) {
                    lastIncomeDate = transaction.transactionDate();
                }
            } else {
                expense = expense.add(transaction.baseAmount());
                values[1] = values[1].add(transaction.baseAmount());
            }
        }

        ClientSummary summarize(Long clientId, List<YearMonth> months, BigDecimal totalIncome) {
            List<MonthlyValues> monthly = new ArrayList<>();
            int activeMonths = 0;
            for (YearMonth month : months) {
                BigDecimal[] values =
                        byMonth.getOrDefault(
                                month, new BigDecimal[] {BigDecimal.ZERO, BigDecimal.ZERO});
                if (values[0].signum() > 0) {
                    activeMonths++;
                }
                monthly.add(new MonthlyValues(month, values[0], values[1]));
            }
            return new ClientSummary(
                    clientId,
                    income,
                    expense,
                    income.subtract(expense),
                    incomeCount,
                    average(income, incomeCount),
                    share(income, totalIncome),
                    activeMonths,
                    lastIncomeDate,
                    monthly);
        }
    }
}
