package com.lmf.finpro.domain.model;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/** Cálculo puro dos totais por tag, a partir das transações já filtradas e das tags de cada uma. */
public final class TagTotalsCalculator {

    private TagTotalsCalculator() {}

    /**
     * @param tags tags que viram linha. Tag sem nenhuma transação no período fica de fora, a menos
     *     que {@code keepEmpty} (o usuário escolheu essa tag no filtro e espera vê-la, mesmo
     *     zerada)
     * @param tagIdsByTransaction ids das tags de cada transação; transação sem tag não aparece
     */
    public static List<TagTotalsReportData.Row> byTag(
            List<Transaction> transactions,
            Map<Long, List<Long>> tagIdsByTransaction,
            Collection<Tag> tags,
            boolean keepEmpty) {
        return tags.stream()
                .sorted(Comparator.comparing(Tag::name))
                .map(
                        tag ->
                                row(
                                        tag.label(),
                                        transactions,
                                        transaction ->
                                                tagIdsByTransaction
                                                        .getOrDefault(transaction.id(), List.of())
                                                        .contains(tag.id())))
                .filter(row -> keepEmpty || row.count() > 0)
                .toList();
    }

    /** Totais das transações sem nenhuma tag. */
    public static TagTotalsReportData.Row untagged(
            List<Transaction> transactions, Map<Long, List<Long>> tagIdsByTransaction) {
        return row(
                "Sem tag",
                transactions,
                transaction ->
                        tagIdsByTransaction.getOrDefault(transaction.id(), List.of()).isEmpty());
    }

    private static TagTotalsReportData.Row row(
            String label, List<Transaction> transactions, Predicate<Transaction> belongs) {
        int count = 0;
        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expense = BigDecimal.ZERO;
        for (Transaction transaction : transactions) {
            if (!belongs.test(transaction)) {
                continue;
            }
            count++;
            if (transaction.type() == CategoryType.INCOME) {
                income = income.add(transaction.baseAmount());
            } else {
                expense = expense.add(transaction.baseAmount());
            }
        }
        return new TagTotalsReportData.Row(label, count, income, expense, income.subtract(expense));
    }
}
