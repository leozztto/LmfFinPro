package com.lmf.finpro.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TagTotalsCalculatorTest {

    private static final Tag ACME = new Tag(1L, 10L, "site-acme", null, LocalDateTime.now());
    private static final Tag DEDUCTIBLE = new Tag(2L, 10L, "dedutível", null, LocalDateTime.now());
    private static final Tag UNUSED = new Tag(3L, 10L, "sem-uso", null, LocalDateTime.now());

    private static Transaction transaction(long id, String amount, CategoryType type) {
        return new Transaction(
                id,
                1L,
                null,
                null,
                "t" + id,
                new BigDecimal(amount),
                LocalDate.of(2026, 9, 1),
                type,
                TransactionOrigin.MANUAL,
                LocalDateTime.now(),
                null,
                null);
    }

    private final List<Transaction> transactions =
            List.of(
                    transaction(1, "5000", CategoryType.INCOME), // #site-acme
                    transaction(2, "800", CategoryType.EXPENSE), // #site-acme #dedutível
                    transaction(3, "200", CategoryType.EXPENSE), // #dedutível
                    transaction(4, "50", CategoryType.EXPENSE)); // sem tag
    private final Map<Long, List<Long>> tagIdsByTransaction =
            Map.of(1L, List.of(1L), 2L, List.of(1L, 2L), 3L, List.of(2L));

    @Test
    void aTransactionWithTwoTagsCountsInBoth() {
        List<TagTotalsReportData.Row> rows =
                TagTotalsCalculator.byTag(
                        transactions, tagIdsByTransaction, List.of(ACME, DEDUCTIBLE), false);

        assertThat(rows)
                .extracting(TagTotalsReportData.Row::label)
                .containsExactly("#dedutível", "#site-acme");
        TagTotalsReportData.Row deductible = rows.get(0);
        assertThat(deductible.count()).isEqualTo(2);
        assertThat(deductible.income()).isEqualByComparingTo("0");
        assertThat(deductible.expense()).isEqualByComparingTo("1000");
        assertThat(deductible.result()).isEqualByComparingTo("-1000");
        TagTotalsReportData.Row acme = rows.get(1);
        assertThat(acme.count()).isEqualTo(2);
        assertThat(acme.result()).isEqualByComparingTo("4200");
    }

    @Test
    void tagWithoutMovementOnlyShowsWhenChosenInTheFilter() {
        assertThat(
                        TagTotalsCalculator.byTag(
                                transactions, tagIdsByTransaction, List.of(UNUSED), false))
                .isEmpty();
        assertThat(
                        TagTotalsCalculator.byTag(
                                transactions, tagIdsByTransaction, List.of(UNUSED), true))
                .singleElement()
                .satisfies(row -> assertThat(row.count()).isZero());
    }

    @Test
    void untaggedRowSumsOnlyTransactionsWithoutTags() {
        TagTotalsReportData.Row untagged =
                TagTotalsCalculator.untagged(transactions, tagIdsByTransaction);

        assertThat(untagged.label()).isEqualTo("Sem tag");
        assertThat(untagged.count()).isEqualTo(1);
        assertThat(untagged.expense()).isEqualByComparingTo("50");
    }
}
