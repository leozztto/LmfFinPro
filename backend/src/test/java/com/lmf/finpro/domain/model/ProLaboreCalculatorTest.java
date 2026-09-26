package com.lmf.finpro.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ProLaboreCalculatorTest {

    @Test
    void monthIncomeBaseDiscountsExpensesTaxReserveAndWithdrawals() {
        // Orçamento: 10000 − 1700 − 600 (6%) − 1000 (10%) = 6700; já retirado 500 → 6200.
        ProLaboreCalculator.Result result =
                ProLaboreCalculator.calculate(
                        inputs(monthIncome("0.10", null), "20000", "10000", "1700", "1700", "500"));

        assertThat(result.taxReserve()).isEqualByComparingTo("600");
        assertThat(result.reserve()).isEqualByComparingTo("1000");
        assertThat(result.monthBudget()).isEqualByComparingTo("6700");
        assertThat(result.balanceCap()).isEqualByComparingTo("18200");
        assertThat(result.cappedByBalance()).isFalse();
        assertThat(result.availableToWithdraw()).isEqualByComparingTo("6200");
        assertThat(result.suggestedPayment()).isEqualByComparingTo("6200");
        assertThat(result.withholdingApplied()).isFalse();
        assertThat(result.payroll().net()).isEqualByComparingTo("6700");
    }

    @Test
    void monthIncomeBaseIsCappedByWhatTheBalanceAllows() {
        // Recebeu 10000 mas a conta só tem 3000: teto = 3000 − 0 pendente − 600 de imposto.
        ProLaboreCalculator.Result result =
                ProLaboreCalculator.calculate(
                        inputs(monthIncome("0", null), "3000", "10000", "0", "0", "0"));

        assertThat(result.calculatedAmount()).isEqualByComparingTo("9400");
        assertThat(result.balanceCap()).isEqualByComparingTo("2400");
        assertThat(result.cappedByBalance()).isTrue();
        assertThat(result.availableToWithdraw()).isEqualByComparingTo("2400");
    }

    @Test
    void monthIncomeBaseNeverGoesNegative() {
        ProLaboreCalculator.Result result =
                ProLaboreCalculator.calculate(
                        inputs(monthIncome("0.10", null), "20000", "1000", "3000", "0", "0"));

        assertThat(result.calculatedAmount()).isNegative();
        assertThat(result.cappedByBalance()).isFalse();
        assertThat(result.monthBudget()).isEqualByComparingTo("0");
        assertThat(result.availableToWithdraw()).isEqualByComparingTo("0");
    }

    @Test
    void currentBalanceBaseKeepsTheTaxBoxAndTheCashCushion() {
        ProLaboreSettings settings =
                new ProLaboreSettings(
                        1L,
                        ProLaboreCalculationBase.CURRENT_BALANCE,
                        2,
                        new BigDecimal("0.10"),
                        ProLaboreTaxMode.AUTOMATIC,
                        null,
                        null,
                        ProLaboreWithholdingMode.DISABLED,
                        null);
        ProLaboreCalculator.Result result =
                ProLaboreCalculator.calculate(
                        new ProLaboreCalculator.Inputs(
                                settings,
                                new BigDecimal("20000"),
                                new BigDecimal("10000"),
                                new BigDecimal("1700"),
                                new BigDecimal("1700"),
                                new BigDecimal("0.06"),
                                new BigDecimal("5000"),
                                new BigDecimal("3000"),
                                new BigDecimal("999"),
                                false,
                                BigDecimal.ZERO));

        // Sobra no caixa: 20000 − 1700 − max(600, 5000) − 2 × 3000 = 7300. O já retirado (999)
        // saiu do saldo, então entra no orçamento do mês e sai de novo do disponível.
        assertThat(result.taxReserve()).isEqualByComparingTo("5000");
        assertThat(result.cashCushion()).isEqualByComparingTo("6000");
        assertThat(result.reserve()).isEqualByComparingTo("0");
        assertThat(result.balanceCap()).isNull();
        assertThat(result.monthBudget()).isEqualByComparingTo("8299");
        assertThat(result.availableToWithdraw()).isEqualByComparingTo("7300");
    }

    @Test
    void fixedAmountSuggestsWhatIsLeftOfItWhenCovered() {
        ProLaboreCalculator.Result result =
                ProLaboreCalculator.calculate(
                        inputs(monthIncome("0", "5000"), "20000", "10000", "0", "0", "2000"));

        // Orçamento 10000 − 600 = 9400; falta pagar 5000 − 2000 = 3000 do fixo.
        assertThat(result.availableToWithdraw()).isEqualByComparingTo("7400");
        assertThat(result.fixedRemaining()).isEqualByComparingTo("3000");
        assertThat(result.fixedCovered()).isTrue();
        assertThat(result.suggestedPayment()).isEqualByComparingTo("3000");
    }

    @Test
    void fixedAmountNotCoveredSuggestsOnlyWhatIsAvailable() {
        ProLaboreCalculator.Result result =
                ProLaboreCalculator.calculate(
                        inputs(monthIncome("0", "8000"), "20000", "5000", "0", "0", "0"));

        // Orçamento 5000 − 300 = 4700 < 8000 do fixo.
        assertThat(result.fixedCovered()).isFalse();
        assertThat(result.suggestedPayment()).isEqualByComparingTo("4700");
    }

    @Test
    void withholdingSplitsTheBudgetIntoGrossTaxesAndNet() {
        ProLaboreCalculator.Result result =
                ProLaboreCalculator.calculate(
                        withholding(
                                inputs(monthIncome("0", null), "20000", "10000", "0", "0", "0"),
                                "0.20"));

        // Orçamento 9400 cobre bruto + 20% patronal: bruto = 9400 ÷ 1,2 = 7833,33.
        // INSS 861,67; IRRF (base 6971,66, sem redutor acima de 7350) 1008,48; patronal 1566,67.
        assertThat(result.monthBudget()).isEqualByComparingTo("9400");
        assertThat(result.payroll().gross()).isEqualByComparingTo("7833.33");
        assertThat(result.payroll().employeeInss()).isEqualByComparingTo("861.67");
        assertThat(result.payroll().irrf()).isEqualByComparingTo("1008.48");
        assertThat(result.payroll().employerInss()).isEqualByComparingTo("1566.67");
        assertThat(result.payroll().net()).isEqualByComparingTo("5963.18");
        assertThat(result.availableToWithdraw()).isEqualByComparingTo("5963.18");
    }

    @Test
    void withholdingWithFixedAmountComparesNetWithWhatWasAlreadyWithdrawn() {
        ProLaboreCalculator.Result result =
                ProLaboreCalculator.calculate(
                        withholding(
                                inputs(
                                        monthIncome("0", "5000"),
                                        "20000",
                                        "10000",
                                        "0",
                                        "0",
                                        "1000"),
                                "0"));

        // Fixo 5000 → INSS 550, IRRF zero (até 5000), líquido 4450; já retirado 1000.
        assertThat(result.payroll().gross()).isEqualByComparingTo("5000");
        assertThat(result.payroll().net()).isEqualByComparingTo("4450");
        assertThat(result.fixedRemaining()).isEqualByComparingTo("3450");
        assertThat(result.fixedCovered()).isTrue();
        assertThat(result.suggestedPayment()).isEqualByComparingTo("3450");
    }

    @Test
    void nonPositiveFixedAmountMeansNoFixedAmount() {
        ProLaboreCalculator.Result result =
                ProLaboreCalculator.calculate(
                        inputs(monthIncome("0", "0"), "20000", "5000", "0", "0", "0"));

        assertThat(result.fixedRemaining()).isNull();
        assertThat(result.suggestedPayment()).isEqualByComparingTo(result.availableToWithdraw());
    }

    private static ProLaboreSettings monthIncome(String reserveRate, String fixedAmount) {
        return new ProLaboreSettings(
                1L,
                ProLaboreCalculationBase.MONTH_INCOME,
                1,
                new BigDecimal(reserveRate),
                ProLaboreTaxMode.AUTOMATIC,
                null,
                fixedAmount == null ? null : new BigDecimal(fixedAmount),
                ProLaboreWithholdingMode.AUTOMATIC,
                null);
    }

    /** Alíquota fixa de 6%, sem caixinha, sem média de despesas e sem retenção. */
    private static ProLaboreCalculator.Inputs inputs(
            ProLaboreSettings settings,
            String balance,
            String income,
            String monthExpenses,
            String pending,
            String withdrawn) {
        return new ProLaboreCalculator.Inputs(
                settings,
                new BigDecimal(balance),
                new BigDecimal(income),
                new BigDecimal(monthExpenses),
                new BigDecimal(pending),
                new BigDecimal("0.06"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal(withdrawn),
                false,
                BigDecimal.ZERO);
    }

    private static ProLaboreCalculator.Inputs withholding(
            ProLaboreCalculator.Inputs in, String employerRate) {
        return new ProLaboreCalculator.Inputs(
                in.settings(),
                in.businessBalance(),
                in.monthIncome(),
                in.monthExpenses(),
                in.pendingExpenses(),
                in.taxRate(),
                in.taxReserveSaved(),
                in.averageMonthlyExpense(),
                in.withdrawnThisMonth(),
                true,
                new BigDecimal(employerRate));
    }
}
