package com.lmf.finpro.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.lmf.finpro.domain.model.NetWorthCalculator.Point;
import com.lmf.finpro.domain.model.NetWorthCalculator.Report;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;

class NetWorthCalculatorTest {

    private static final YearMonth JANUARY = YearMonth.of(2026, 1);
    private static final YearMonth FEBRUARY = YearMonth.of(2026, 2);
    private static final YearMonth MARCH = YearMonth.of(2026, 3);

    private static final Account CHECKING =
            new Account(1L, 10L, "Corrente", AccountType.CHECKING, new BigDecimal("2000"), null);
    private static final Account BROKER =
            new Account(2L, 10L, "Corretora", AccountType.INVESTMENT, BigDecimal.ZERO, null);
    private static final Debt CAR = new Debt(5L, 10L, "Carro", DebtType.FINANCING, "Banco", null);

    @Test
    void consolidatesCashInvestmentsAndDebtsMonthByMonth() {
        // Em fevereiro transferiu 1000 da corrente para a corretora, que valia 1050 em 28/02.
        List<Transaction> transactions =
                List.of(
                        paid(1L, CategoryType.INCOME, "500", "2026-01-15"),
                        paid(1L, CategoryType.EXPENSE, "1000", "2026-02-10"),
                        paid(2L, CategoryType.INCOME, "1000", "2026-02-10"),
                        paid(1L, CategoryType.EXPENSE, "300", "2026-03-05"));
        List<AccountValuation> valuations =
                List.of(
                        new AccountValuation(
                                null, 2L, LocalDate.of(2026, 2, 28), new BigDecimal("1050"), null));
        List<DebtBalance> balances =
                List.of(
                        new DebtBalance(
                                null, 5L, LocalDate.of(2026, 1, 20), new BigDecimal("900"), null),
                        new DebtBalance(
                                null, 5L, LocalDate.of(2026, 3, 1), new BigDecimal("800"), null));

        Report report =
                NetWorthCalculator.calculate(
                        List.of(CHECKING, BROKER),
                        transactions,
                        valuations,
                        List.of(CAR),
                        balances,
                        List.of(JANUARY, FEBRUARY, MARCH),
                        MARCH);

        assertThat(report.history())
                .extracting(Point::month)
                .containsExactly(JANUARY, FEBRUARY, MARCH);
        Point january = report.history().get(0);
        assertThat(january.cash()).isEqualByComparingTo("2500");
        assertThat(january.investments()).isEqualByComparingTo("0");
        assertThat(january.debts()).isEqualByComparingTo("900");
        assertThat(january.netWorth()).isEqualByComparingTo("1600");
        Point february = report.history().get(1);
        assertThat(february.cash()).isEqualByComparingTo("1500");
        assertThat(february.investments()).isEqualByComparingTo("1050");
        assertThat(february.netWorth()).isEqualByComparingTo("1650");
        Point march = report.current();
        assertThat(march.cash()).isEqualByComparingTo("1200");
        assertThat(march.debts()).isEqualByComparingTo("800");
        assertThat(march.netWorth()).isEqualByComparingTo("1450");
        assertThat(report.changeFromPreviousMonth()).isEqualByComparingTo("-200");
    }

    @Test
    void describesEachInvestmentDebtAndAccount() {
        List<Transaction> transactions =
                List.of(
                        paid(2L, CategoryType.INCOME, "1000", "2026-02-10"),
                        paid(2L, CategoryType.EXPENSE, "200", "2026-03-01"));
        AccountValuation valuation =
                new AccountValuation(
                        9L, 2L, LocalDate.of(2026, 3, 10), new BigDecimal("880"), null);
        DebtBalance balance =
                new DebtBalance(3L, 5L, LocalDate.of(2026, 3, 1), new BigDecimal("800"), null);

        Report report =
                NetWorthCalculator.calculate(
                        List.of(CHECKING, BROKER),
                        transactions,
                        List.of(valuation),
                        List.of(CAR),
                        List.of(balance),
                        List.of(MARCH),
                        MARCH);

        NetWorthCalculator.InvestmentRow investment = report.investments().get(0);
        assertThat(investment.invested()).isEqualByComparingTo("800");
        assertThat(investment.currentValue()).isEqualByComparingTo("880");
        assertThat(investment.gain()).isEqualByComparingTo("80");
        assertThat(investment.gainRate()).isEqualByComparingTo("0.1");
        assertThat(investment.lastValuation()).isEqualTo(valuation);
        assertThat(report.investmentGain()).isEqualByComparingTo("80");
        assertThat(report.accounts()).hasSize(1);
        assertThat(report.accounts().get(0).balance()).isEqualByComparingTo("2000");
        assertThat(report.debts().get(0).currentBalance()).isEqualByComparingTo("800");
        assertThat(report.debts().get(0).lastBalance()).isEqualTo(balance);
        assertThat(report.changeFromPreviousMonth()).isNull();
    }

    @Test
    void debtStartsAtZeroBeforeItsFirstBalanceAndInvestmentWithoutMoneyHasNoRate() {
        Report report =
                NetWorthCalculator.calculate(
                        List.of(BROKER),
                        List.of(),
                        List.of(),
                        List.of(CAR),
                        List.of(
                                new DebtBalance(
                                        null,
                                        5L,
                                        LocalDate.of(2026, 3, 1),
                                        new BigDecimal("800"),
                                        null)),
                        List.of(FEBRUARY, MARCH),
                        MARCH);

        assertThat(report.history().get(0).debts()).isEqualByComparingTo("0");
        assertThat(report.current().debts()).isEqualByComparingTo("800");
        assertThat(report.investments().get(0).gainRate()).isNull();
    }

    private static Transaction paid(Long accountId, CategoryType type, String amount, String date) {
        return new Transaction(
                null,
                accountId,
                null,
                null,
                "Movimento",
                new BigDecimal(amount),
                LocalDate.parse(date),
                type,
                TransactionOrigin.MANUAL,
                null,
                null,
                null,
                null,
                TransactionStatus.PAID);
    }
}
