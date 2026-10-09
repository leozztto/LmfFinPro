package com.lmf.finpro.domain.model;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Detecta os insights automáticos a partir dos lançamentos: assinaturas possivelmente esquecidas,
 * despesas fora do padrão da categoria e clientes que atrasam os pagamentos. Classe pura — recebe
 * as transações e a data de hoje, sem acesso a banco ou a relógio.
 *
 * <p>"Cliente que atrasa" usa {@code paidAt} (dia em que a pendência foi marcada como paga).
 * Recebimentos sem essa data (nasceram pagos ou são anteriores ao campo) ficam de fora.
 */
public final class InsightDetector {

    /** Meses seguidos de cobrança parecida para considerar assinatura. */
    static final int SUBSCRIPTION_MIN_MONTHS = 4;

    /** Quanto o maior valor pode superar o menor para a cobrança ainda contar como "a mesma". */
    private static final BigDecimal SUBSCRIPTION_MAX_VARIATION = new BigDecimal("1.10");

    /** Janela, em dias, das despesas avaliadas como fora do padrão (as mais recentes). */
    static final int UNUSUAL_RECENT_DAYS = 7;

    /** Histórico da categoria usado como base de comparação. */
    static final int UNUSUAL_HISTORY_MONTHS = 6;

    static final int UNUSUAL_MIN_SAMPLES = 5;
    static final BigDecimal UNUSUAL_MIN_AMOUNT = new BigDecimal("50");
    private static final BigDecimal UNUSUAL_STD_FACTOR = new BigDecimal("2");
    private static final BigDecimal UNUSUAL_MEAN_FACTOR = new BigDecimal("1.5");

    /** Até quantos dias atrás um recebimento vencido e pendente conta como atraso do cliente. */
    static final int LATE_CLIENT_WINDOW_DAYS = 90;

    static final int LATE_CLIENT_MIN_LATE = 2;

    /** Parcela mínima de recebimentos atrasados, entre os de desfecho conhecido. */
    private static final BigDecimal LATE_CLIENT_MIN_SHARE = new BigDecimal("0.5");

    private InsightDetector() {}

    public static List<Insight> detect(
            List<Transaction> transactions,
            LocalDate today,
            Function<Long, String> categoryName,
            Function<Long, String> clientName) {
        List<Insight> insights = new ArrayList<>();
        insights.addAll(forgottenSubscriptions(transactions, today));
        insights.addAll(unusualExpenses(transactions, today, categoryName));
        insights.addAll(lateClients(transactions, today, clientName));
        return insights;
    }

    /**
     * Despesa paga e fora de qualquer recorrência cadastrada, cobrada uma vez por mês em pelo menos
     * {@link #SUBSCRIPTION_MIN_MONTHS} meses seguidos, com valor estável, e ainda cobrada no mês
     * atual ou no anterior. Se o usuário cadastrou a recorrência, ele sabe da cobrança — não entra.
     */
    static List<Insight> forgottenSubscriptions(List<Transaction> transactions, LocalDate today) {
        YearMonth current = YearMonth.from(today);
        Map<String, List<Transaction>> byDescription =
                transactions.stream()
                        .filter(t -> t.type() == CategoryType.EXPENSE)
                        .filter(t -> t.transferId() == null)
                        .filter(t -> t.recurringTransactionId() == null)
                        .filter(Transaction::isPaid)
                        .filter(t -> !t.transactionDate().isAfter(today))
                        .filter(t -> t.description() != null)
                        .collect(
                                Collectors.groupingBy(
                                        t -> normalize(t.description()),
                                        LinkedHashMap::new,
                                        Collectors.toList()));

        List<Insight> insights = new ArrayList<>();
        byDescription.forEach(
                (name, group) -> {
                    if (name.isBlank()) {
                        return;
                    }
                    Map<YearMonth, List<Transaction>> byMonth =
                            group.stream()
                                    .collect(
                                            Collectors.groupingBy(
                                                    t -> YearMonth.from(t.transactionDate())));
                    YearMonth latest = new TreeSet<>(byMonth.keySet()).last();
                    if (latest.isBefore(current.minusMonths(1))) {
                        return;
                    }
                    List<Transaction> streak = new ArrayList<>();
                    for (YearMonth month = latest;
                            byMonth.containsKey(month);
                            month = month.minusMonths(1)) {
                        List<Transaction> charges = byMonth.get(month);
                        if (charges.size() != 1) {
                            break;
                        }
                        streak.add(charges.get(0));
                    }
                    if (streak.size() < SUBSCRIPTION_MIN_MONTHS || !stable(streak)) {
                        return;
                    }
                    Transaction last = streak.get(0);
                    insights.add(
                            new Insight(
                                    AlertType.INSIGHT_SUBSCRIPTION,
                                    key(name, 40) + "-" + latest.getYear(),
                                    last.description(),
                                    last.baseAmount(),
                                    null,
                                    streak.size()));
                });
        insights.sort(Comparator.comparing(Insight::amount).reversed());
        return insights;
    }

    /**
     * Despesa dos últimos {@link #UNUSUAL_RECENT_DAYS} dias que supera, na mesma categoria, a média
     * dos últimos {@link #UNUSUAL_HISTORY_MONTHS} meses em mais de dois desvios-padrão (e em pelo
     * menos 50%), com valor mínimo e amostra mínima para não avisar sobre ruído.
     */
    static List<Insight> unusualExpenses(
            List<Transaction> transactions, LocalDate today, Function<Long, String> categoryName) {
        LocalDate recentStart = today.minusDays(UNUSUAL_RECENT_DAYS);
        LocalDate historyStart = today.minusMonths(UNUSUAL_HISTORY_MONTHS);

        List<Transaction> expenses =
                transactions.stream()
                        .filter(t -> t.type() == CategoryType.EXPENSE)
                        .filter(t -> t.transferId() == null)
                        .filter(t -> t.categoryId() != null)
                        .toList();
        Map<Long, List<BigDecimal>> history =
                expenses.stream()
                        .filter(t -> t.transactionDate().isBefore(recentStart))
                        .filter(t -> !t.transactionDate().isBefore(historyStart))
                        .collect(
                                Collectors.groupingBy(
                                        Transaction::categoryId,
                                        Collectors.mapping(
                                                Transaction::baseAmount, Collectors.toList())));

        List<Insight> insights = new ArrayList<>();
        for (Transaction expense : expenses) {
            LocalDate date = expense.transactionDate();
            if (date.isBefore(recentStart)
                    || date.isAfter(today)
                    || expense.recurringTransactionId() != null
                    || expense.baseAmount().compareTo(UNUSUAL_MIN_AMOUNT) < 0) {
                continue;
            }
            List<BigDecimal> samples = history.getOrDefault(expense.categoryId(), List.of());
            if (samples.size() < UNUSUAL_MIN_SAMPLES) {
                continue;
            }
            BigDecimal mean = mean(samples);
            BigDecimal threshold =
                    mean.add(standardDeviation(samples, mean).multiply(UNUSUAL_STD_FACTOR))
                            .max(mean.multiply(UNUSUAL_MEAN_FACTOR));
            if (expense.baseAmount().compareTo(threshold) <= 0) {
                continue;
            }
            insights.add(
                    new Insight(
                            AlertType.INSIGHT_UNUSUAL_EXPENSE,
                            expense.id().toString(),
                            expense.description()
                                    + " ("
                                    + categoryName.apply(expense.categoryId())
                                    + ")",
                            expense.baseAmount(),
                            mean.setScale(2, RoundingMode.HALF_UP),
                            0));
        }
        insights.sort(Comparator.comparing(Insight::amount).reversed());
        return insights;
    }

    /**
     * Cliente que atrasa: entre os recebimentos dele nos últimos {@link #LATE_CLIENT_WINDOW_DAYS}
     * dias, conta os atrasados — pendentes e já vencidos, ou pagos depois do vencimento ({@code
     * paidAt} depois de {@code transactionDate}) — e avisa quando são pelo menos {@link
     * #LATE_CLIENT_MIN_LATE} e a maioria ({@link #LATE_CLIENT_MIN_SHARE}) do que tem desfecho
     * conhecido. Recebimento sem {@code paidAt} (nasceu pago ou é anterior ao campo) não entra nem
     * como atraso nem como pontual, porque não se sabe. A chave inclui o mês: enquanto o padrão
     * persistir, o aviso se repete uma vez por mês, não todo dia.
     */
    static List<Insight> lateClients(
            List<Transaction> transactions, LocalDate today, Function<Long, String> clientName) {
        LocalDate oldest = today.minusDays(LATE_CLIENT_WINDOW_DAYS);
        Map<Long, List<Transaction>> receivablesByClient =
                transactions.stream()
                        .filter(t -> t.type() == CategoryType.INCOME)
                        .filter(t -> t.transferId() == null)
                        .filter(t -> t.clientId() != null)
                        .filter(t -> outcomeKnown(t, today, oldest))
                        .collect(Collectors.groupingBy(Transaction::clientId));

        String month = YearMonth.from(today).toString();
        List<Insight> insights = new ArrayList<>();
        receivablesByClient.forEach(
                (clientId, receivables) -> {
                    List<Transaction> late =
                            receivables.stream().filter(t -> isLate(t, today)).toList();
                    if (late.size() < LATE_CLIENT_MIN_LATE
                            || BigDecimal.valueOf(late.size())
                                            .divide(
                                                    BigDecimal.valueOf(receivables.size()),
                                                    MathContext.DECIMAL64)
                                            .compareTo(LATE_CLIENT_MIN_SHARE)
                                    < 0) {
                        return;
                    }
                    BigDecimal total =
                            late.stream()
                                    .map(Transaction::baseAmount)
                                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                    insights.add(
                            new Insight(
                                    AlertType.INSIGHT_LATE_CLIENT,
                                    clientId + "-" + month,
                                    clientName.apply(clientId),
                                    total,
                                    null,
                                    late.size()));
                });
        insights.sort(Comparator.comparing(Insight::amount).reversed());
        return insights;
    }

    /**
     * Recebimento com desfecho conhecido na janela: pago com a data do pagamento registrada, ou
     * pendente (e já vencido). Pendente ainda a vencer e pago sem data ficam de fora.
     */
    private static boolean outcomeKnown(Transaction t, LocalDate today, LocalDate oldest) {
        if (t.isPaid()) {
            return t.paidAt() != null && !t.paidAt().isBefore(oldest);
        }
        return t.transactionDate().isBefore(today) && !t.transactionDate().isBefore(oldest);
    }

    private static boolean isLate(Transaction t, LocalDate today) {
        return t.isPaid()
                ? t.paidAt().isAfter(t.transactionDate())
                : t.transactionDate().isBefore(today);
    }

    private static boolean stable(List<Transaction> charges) {
        BigDecimal min =
                charges.stream().map(Transaction::baseAmount).min(Comparator.naturalOrder()).get();
        BigDecimal max =
                charges.stream().map(Transaction::baseAmount).max(Comparator.naturalOrder()).get();
        return min.signum() > 0 && max.compareTo(min.multiply(SUBSCRIPTION_MAX_VARIATION)) <= 0;
    }

    private static BigDecimal mean(List<BigDecimal> values) {
        return values.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(values.size()), MathContext.DECIMAL64);
    }

    private static BigDecimal standardDeviation(List<BigDecimal> values, BigDecimal mean) {
        BigDecimal variance =
                values.stream()
                        .map(v -> v.subtract(mean).pow(2))
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(values.size()), MathContext.DECIMAL64);
        return variance.sqrt(MathContext.DECIMAL64);
    }

    /**
     * Minúsculas, sem acentos, sem dígitos e com espaços colapsados: "Netflix 03/24" = "netflix".
     */
    static String normalize(String description) {
        String decomposed = Normalizer.normalize(description, Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}", "").toLowerCase().replaceAll("[^a-z]+", " ").trim();
    }

    private static String key(String value, int maxLength) {
        String safe = value.replace(' ', '_');
        return safe.length() <= maxLength ? safe : safe.substring(0, maxLength);
    }
}
