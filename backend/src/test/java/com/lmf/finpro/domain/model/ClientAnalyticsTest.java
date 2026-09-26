package com.lmf.finpro.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;

class ClientAnalyticsTest {

    private static final List<YearMonth> MONTHS =
            List.of(YearMonth.of(2026, 7), YearMonth.of(2026, 8), YearMonth.of(2026, 9));
    private static final Long A = 1L;
    private static final Long B = 2L;
    private static final Long C = 3L;

    @Test
    void ranksClientsAndMeasuresConcentrationOverTheWholeIncome() {
        ClientAnalytics.Report report =
                ClientAnalytics.analyze(
                        List.of(
                                income(A, "7000", LocalDate.of(2026, 7, 10)),
                                income(A, "7000", LocalDate.of(2026, 9, 10)),
                                income(B, "2000", LocalDate.of(2026, 8, 5)),
                                expense(B, "300", LocalDate.of(2026, 8, 6)),
                                income(null, "1000", LocalDate.of(2026, 9, 1)),
                                income(A, "5000", LocalDate.of(2026, 6, 30)),
                                expense(C, "100", LocalDate.of(2026, 9, 2))),
                        MONTHS);

        // Total 17000 (a receita de junho está fora do período; a sem cliente entra no total).
        assertThat(report.totalIncome()).isEqualByComparingTo("17000");
        assertThat(report.unassignedIncome()).isEqualByComparingTo("1000");
        assertThat(report.activeClients()).isEqualTo(2);
        // Ticket médio: 16000 com cliente ÷ 3 recebimentos.
        assertThat(report.averageTicket()).isEqualByComparingTo("5333.33");
        assertThat(report.topClientShare()).isEqualByComparingTo("0.8235");
        assertThat(report.topThreeShare()).isEqualByComparingTo("0.9412");
        assertThat(report.risk()).isEqualTo(ConcentrationRisk.HIGH);

        assertThat(report.ranking())
                .extracting(ClientAnalytics.ClientSummary::clientId)
                .containsExactly(A, B, C);
        ClientAnalytics.ClientSummary a = report.ranking().get(0);
        assertThat(a.income()).isEqualByComparingTo("14000");
        assertThat(a.incomeCount()).isEqualTo(2);
        assertThat(a.averageTicket()).isEqualByComparingTo("7000");
        assertThat(a.activeMonths()).isEqualTo(2);
        assertThat(a.lastIncomeDate()).isEqualTo(LocalDate.of(2026, 9, 10));
        assertThat(a.monthly())
                .extracting(ClientAnalytics.MonthlyValues::income)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("7000"), BigDecimal.ZERO, new BigDecimal("7000"));

        ClientAnalytics.ClientSummary b = report.ranking().get(1);
        assertThat(b.net()).isEqualByComparingTo("1700");
        ClientAnalytics.ClientSummary c = report.ranking().get(2);
        assertThat(c.income()).isEqualByComparingTo("0");
        assertThat(c.net()).isEqualByComparingTo("-100");
        assertThat(c.averageTicket()).isEqualByComparingTo("0");
    }

    @Test
    void noIncomeMeansNoConcentrationRisk() {
        ClientAnalytics.Report report =
                ClientAnalytics.analyze(
                        List.of(expense(A, "100", LocalDate.of(2026, 9, 2))), MONTHS);

        assertThat(report.totalIncome()).isEqualByComparingTo("0");
        assertThat(report.topClientShare()).isEqualByComparingTo("0");
        assertThat(report.risk()).isEqualTo(ConcentrationRisk.NONE);
        assertThat(report.activeClients()).isZero();
    }

    @Test
    void riskThresholds() {
        assertThat(ConcentrationRisk.of(new BigDecimal("0.2999"), true))
                .isEqualTo(ConcentrationRisk.LOW);
        assertThat(ConcentrationRisk.of(new BigDecimal("0.30"), true))
                .isEqualTo(ConcentrationRisk.MODERATE);
        assertThat(ConcentrationRisk.of(new BigDecimal("0.4999"), true))
                .isEqualTo(ConcentrationRisk.MODERATE);
        assertThat(ConcentrationRisk.of(new BigDecimal("0.50"), true))
                .isEqualTo(ConcentrationRisk.HIGH);
        assertThat(ConcentrationRisk.of(BigDecimal.ZERO, false)).isEqualTo(ConcentrationRisk.NONE);
    }

    private static Transaction income(Long clientId, String amount, LocalDate date) {
        return tx(clientId, amount, date, CategoryType.INCOME);
    }

    private static Transaction expense(Long clientId, String amount, LocalDate date) {
        return tx(clientId, amount, date, CategoryType.EXPENSE);
    }

    private static Transaction tx(Long clientId, String amount, LocalDate date, CategoryType type) {
        return new Transaction(
                null,
                10L,
                null,
                clientId,
                "Movimento",
                new BigDecimal(amount),
                date,
                type,
                TransactionOrigin.MANUAL,
                null,
                null,
                null,
                null,
                TransactionStatus.PAID);
    }
}
