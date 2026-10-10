package com.lmf.finpro.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class InsightDetectorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 18);
    private static long nextId = 1;

    @Test
    void normalizeIgnoresCaseAccentsAndDigits() {
        assertThat(InsightDetector.normalize("  Netflix  03/24 ")).isEqualTo("netflix");
        assertThat(InsightDetector.normalize("Assinatura Pão")).isEqualTo("assinatura pao");
    }

    @Test
    void detectsSubscriptionChargedFourMonthsInARow() {
        List<Transaction> transactions =
                List.of(
                        expense("Netflix", "55.90", TODAY.minusMonths(3), null, null),
                        expense("NETFLIX", "55.90", TODAY.minusMonths(2), null, null),
                        expense("Netflix", "55.90", TODAY.minusMonths(1), null, null),
                        expense("Netflix", "55.90", TODAY, null, null));

        List<Insight> insights = InsightDetector.forgottenSubscriptions(transactions, TODAY);

        assertThat(insights).hasSize(1);
        Insight insight = insights.get(0);
        assertThat(insight.type()).isEqualTo(AlertType.INSIGHT_SUBSCRIPTION);
        assertThat(insight.count()).isEqualTo(4);
        assertThat(insight.amount()).isEqualByComparingTo("55.90");
        assertThat(insight.key()).isEqualTo("netflix-2026").hasSizeLessThanOrEqualTo(50);
    }

    @Test
    void ignoresSubscriptionWithThreeMonthsOnly() {
        List<Transaction> transactions =
                List.of(
                        expense("Netflix", "55.90", TODAY.minusMonths(2), null, null),
                        expense("Netflix", "55.90", TODAY.minusMonths(1), null, null),
                        expense("Netflix", "55.90", TODAY, null, null));

        assertThat(InsightDetector.forgottenSubscriptions(transactions, TODAY)).isEmpty();
    }

    @Test
    void ignoresSubscriptionAlreadyRegisteredAsRecurrence() {
        List<Transaction> transactions =
                List.of(
                        expense("Netflix", "55.90", TODAY.minusMonths(3), null, 1L),
                        expense("Netflix", "55.90", TODAY.minusMonths(2), null, 1L),
                        expense("Netflix", "55.90", TODAY.minusMonths(1), null, 1L),
                        expense("Netflix", "55.90", TODAY, null, 1L));

        assertThat(InsightDetector.forgottenSubscriptions(transactions, TODAY)).isEmpty();
    }

    @Test
    void ignoresDescriptionChargedMoreThanOnceAMonth() {
        List<Transaction> transactions = new ArrayList<>();
        for (int months = 0; months < 5; months++) {
            transactions.add(expense("Mercado", "80", TODAY.minusMonths(months), null, null));
            transactions.add(
                    expense("Mercado", "80", TODAY.minusMonths(months).minusDays(3), null, null));
        }

        assertThat(InsightDetector.forgottenSubscriptions(transactions, TODAY)).isEmpty();
    }

    @Test
    void ignoresSubscriptionWithUnstableAmountOrAlreadyCancelled() {
        List<Transaction> unstable =
                List.of(
                        expense("Luz", "100", TODAY.minusMonths(3), null, null),
                        expense("Luz", "140", TODAY.minusMonths(2), null, null),
                        expense("Luz", "100", TODAY.minusMonths(1), null, null),
                        expense("Luz", "100", TODAY, null, null));
        List<Transaction> cancelled =
                List.of(
                        expense("Spotify", "20", TODAY.minusMonths(6), null, null),
                        expense("Spotify", "20", TODAY.minusMonths(5), null, null),
                        expense("Spotify", "20", TODAY.minusMonths(4), null, null),
                        expense("Spotify", "20", TODAY.minusMonths(3), null, null));

        assertThat(InsightDetector.forgottenSubscriptions(unstable, TODAY)).isEmpty();
        assertThat(InsightDetector.forgottenSubscriptions(cancelled, TODAY)).isEmpty();
    }

    @Test
    void detectsRecentExpenseWellAboveCategoryAverage() {
        List<Transaction> transactions = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            transactions.add(expense("Mercado", "100", TODAY.minusDays(10 * i), 5L, null));
        }
        Transaction outlier = expense("Churrasco", "480", TODAY.minusDays(1), 5L, null);
        transactions.add(outlier);

        List<Insight> insights =
                InsightDetector.unusualExpenses(transactions, TODAY, id -> "Alimentação");

        assertThat(insights).hasSize(1);
        Insight insight = insights.get(0);
        assertThat(insight.type()).isEqualTo(AlertType.INSIGHT_UNUSUAL_EXPENSE);
        assertThat(insight.key()).isEqualTo(outlier.id().toString());
        assertThat(insight.subject()).isEqualTo("Churrasco (Alimentação)");
        assertThat(insight.amount()).isEqualByComparingTo("480");
        assertThat(insight.reference()).isEqualByComparingTo("100");
    }

    @Test
    void ignoresUnusualExpenseWithTooFewSamplesOrSmallAmount() {
        List<Transaction> fewSamples = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            fewSamples.add(expense("Mercado", "100", TODAY.minusDays(10 * i), 5L, null));
        }
        fewSamples.add(expense("Churrasco", "480", TODAY.minusDays(1), 5L, null));

        List<Transaction> small = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            small.add(expense("Café", "5", TODAY.minusDays(10 * i), 6L, null));
        }
        small.add(expense("Café especial", "40", TODAY.minusDays(1), 6L, null));

        assertThat(InsightDetector.unusualExpenses(fewSamples, TODAY, id -> "x")).isEmpty();
        assertThat(InsightDetector.unusualExpenses(small, TODAY, id -> "x")).isEmpty();
    }

    @Test
    void ignoresRecentExpenseInsideNormalRange() {
        List<Transaction> transactions = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            transactions.add(
                    expense(
                            "Mercado",
                            i % 2 == 0 ? "90" : "130",
                            TODAY.minusDays(10 * i),
                            5L,
                            null));
        }
        transactions.add(expense("Mercado", "140", TODAY.minusDays(1), 5L, null));

        assertThat(InsightDetector.unusualExpenses(transactions, TODAY, id -> "x")).isEmpty();
    }

    @Test
    void detectsClientWithTwoOrMoreOverdueReceivables() {
        List<Transaction> transactions =
                List.of(
                        income(7L, "1000", TODAY.minusDays(40), TransactionStatus.PENDING),
                        income(7L, "500", TODAY.minusDays(10), TransactionStatus.PENDING),
                        income(7L, "700", TODAY.minusDays(60), TransactionStatus.PAID),
                        income(8L, "900", TODAY.minusDays(5), TransactionStatus.PENDING),
                        income(9L, "300", TODAY.plusDays(5), TransactionStatus.PENDING),
                        income(9L, "300", TODAY.plusDays(35), TransactionStatus.PENDING));

        List<Insight> insights =
                InsightDetector.lateClients(transactions, TODAY, id -> "Cliente " + id);

        assertThat(insights).hasSize(1);
        Insight insight = insights.get(0);
        assertThat(insight.type()).isEqualTo(AlertType.INSIGHT_LATE_CLIENT);
        assertThat(insight.subject()).isEqualTo("Cliente 7");
        assertThat(insight.count()).isEqualTo(2);
        assertThat(insight.amount()).isEqualByComparingTo("1500");
        assertThat(insight.key()).isEqualTo("7-2026-09");
    }

    @Test
    void countsReceivablesPaidAfterDueDateAsLate() {
        List<Transaction> transactions =
                List.of(
                        paidIncome(7L, "800", TODAY.minusDays(50), TODAY.minusDays(35)),
                        paidIncome(7L, "600", TODAY.minusDays(20), TODAY.minusDays(8)),
                        paidIncome(7L, "400", TODAY.minusDays(30), TODAY.minusDays(30)));

        List<Insight> insights = InsightDetector.lateClients(transactions, TODAY, id -> "Acme");

        assertThat(insights).hasSize(1);
        assertThat(insights.get(0).count()).isEqualTo(2);
        assertThat(insights.get(0).amount()).isEqualByComparingTo("1400");
    }

    @Test
    void ignoresClientWhoMostlyPaysOnTime() {
        List<Transaction> transactions =
                List.of(
                        paidIncome(7L, "800", TODAY.minusDays(80), TODAY.minusDays(70)),
                        paidIncome(7L, "600", TODAY.minusDays(60), TODAY.minusDays(60)),
                        paidIncome(7L, "600", TODAY.minusDays(50), TODAY.minusDays(50)),
                        paidIncome(7L, "600", TODAY.minusDays(40), TODAY.minusDays(40)),
                        paidIncome(7L, "600", TODAY.minusDays(30), TODAY.minusDays(32)),
                        paidIncome(7L, "600", TODAY.minusDays(20), TODAY.minusDays(20)));

        assertThat(InsightDetector.lateClients(transactions, TODAY, id -> "Acme")).isEmpty();
    }

    @Test
    void ignoresPaidReceivablesWithoutPaymentDate() {
        List<Transaction> transactions =
                List.of(
                        income(7L, "800", TODAY.minusDays(50), TransactionStatus.PAID),
                        income(7L, "600", TODAY.minusDays(20), TransactionStatus.PAID),
                        income(7L, "500", TODAY.minusDays(5), TransactionStatus.PENDING));

        assertThat(InsightDetector.lateClients(transactions, TODAY, id -> "Acme")).isEmpty();
    }

    @Test
    void withStatusRecordsPaymentDateOnlyWhenBecomingPaid() {
        Transaction pending = income(7L, "500", TODAY.minusDays(5), TransactionStatus.PENDING);

        Transaction paid = pending.withStatus(TransactionStatus.PAID, TODAY);
        assertThat(paid.paidAt()).isEqualTo(TODAY);
        assertThat(paid.withStatus(TransactionStatus.PAID, TODAY.plusDays(3)).paidAt())
                .isEqualTo(TODAY);
        assertThat(paid.withStatus(TransactionStatus.PENDING, TODAY).paidAt()).isNull();
        assertThat(paid.withBaseAmount(BigDecimal.ONE).paidAt()).isEqualTo(TODAY);
    }

    @Test
    void detectCombinesAllInsightsInOrder() {
        List<Transaction> transactions = new ArrayList<>();
        transactions.add(expense("Netflix", "55.90", TODAY.minusMonths(3), null, null));
        transactions.add(expense("Netflix", "55.90", TODAY.minusMonths(2), null, null));
        transactions.add(expense("Netflix", "55.90", TODAY.minusMonths(1), null, null));
        transactions.add(expense("Netflix", "55.90", TODAY, null, null));
        transactions.add(income(7L, "1000", TODAY.minusDays(40), TransactionStatus.PENDING));
        transactions.add(income(7L, "500", TODAY.minusDays(10), TransactionStatus.PENDING));

        List<Insight> insights =
                InsightDetector.detect(transactions, TODAY, id -> "Categoria", id -> "Cliente");

        assertThat(insights)
                .extracting(Insight::type)
                .containsExactly(AlertType.INSIGHT_SUBSCRIPTION, AlertType.INSIGHT_LATE_CLIENT);
    }

    private static Transaction expense(
            String description,
            String amount,
            LocalDate date,
            Long categoryId,
            Long recurringTransactionId) {
        return new Transaction(
                nextId++,
                1L,
                categoryId,
                null,
                description,
                new BigDecimal(amount),
                date,
                CategoryType.EXPENSE,
                TransactionOrigin.MANUAL,
                LocalDateTime.now(),
                null,
                null,
                recurringTransactionId,
                TransactionStatus.PAID);
    }

    private static Transaction paidIncome(
            Long clientId, String amount, LocalDate dueDate, LocalDate paidAt) {
        return income(clientId, amount, dueDate, TransactionStatus.PENDING)
                .withStatus(TransactionStatus.PAID, paidAt);
    }

    private static Transaction income(
            Long clientId, String amount, LocalDate date, TransactionStatus status) {
        return new Transaction(
                nextId++,
                1L,
                null,
                clientId,
                "Serviço",
                new BigDecimal(amount),
                date,
                CategoryType.INCOME,
                TransactionOrigin.MANUAL,
                LocalDateTime.now(),
                null,
                null,
                null,
                status);
    }
}
