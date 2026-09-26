package com.lmf.finpro.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.lmf.finpro.domain.model.PayrollTaxCalculator.PayrollTaxes;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** Valores de referência de 2026 (teto do INSS, tabela do IRRF e redutor da Lei 15.270/2025). */
class PayrollTaxCalculatorTest {

    @Test
    void upToFiveThousandOnlyInssIsDueBecauseOfTheReduction() {
        PayrollTaxes taxes =
                PayrollTaxCalculator.compute(new BigDecimal("5000.00"), BigDecimal.ZERO);

        assertThat(taxes.employeeInss()).isEqualByComparingTo("550.00");
        assertThat(taxes.irrf()).isEqualByComparingTo("0");
        assertThat(taxes.net()).isEqualByComparingTo("4450.00");
    }

    @Test
    void partialReductionBetweenFiveThousandAndSevenThousandThreeHundredFifty() {
        // INSS 660; base 6000 − 660 = 5340 → 27,5% − 908,73 = 559,77;
        // redutor 978,62 − 0,133145 × 6000 = 179,75 → IRRF 380,02.
        PayrollTaxes taxes =
                PayrollTaxCalculator.compute(new BigDecimal("6000.00"), BigDecimal.ZERO);

        assertThat(taxes.employeeInss()).isEqualByComparingTo("660.00");
        assertThat(taxes.irrf()).isEqualByComparingTo("380.02");
        assertThat(taxes.net()).isEqualByComparingTo("4959.98");
    }

    @Test
    void simplifiedDiscountIsUsedWhenLargerThanInss() {
        // INSS 572 < 607,20: base 5200 − 607,20 = 4592,80 → 22,5% − 675,49 = 357,89;
        // redutor 978,62 − 692,35 = 286,27 → IRRF 71,62.
        PayrollTaxes taxes =
                PayrollTaxCalculator.compute(new BigDecimal("5200.00"), BigDecimal.ZERO);

        assertThat(taxes.employeeInss()).isEqualByComparingTo("572.00");
        assertThat(taxes.irrf()).isEqualByComparingTo("71.62");
    }

    @Test
    void inssIsCappedAtTheCeilingAndNoReductionAboveSevenThousandThreeHundredFifty() {
        // INSS 11% do teto 8475,55 = 932,31; base 9067,69 → 27,5% − 908,73 = 1584,88.
        PayrollTaxes taxes =
                PayrollTaxCalculator.compute(new BigDecimal("10000.00"), new BigDecimal("0.20"));

        assertThat(taxes.employeeInss()).isEqualByComparingTo("932.31");
        assertThat(taxes.irrf()).isEqualByComparingTo("1584.88");
        assertThat(taxes.employerInss()).isEqualByComparingTo("2000.00");
        assertThat(taxes.net()).isEqualByComparingTo("7482.81");
        assertThat(taxes.companyCost()).isEqualByComparingTo("12000.00");
        assertThat(taxes.taxesToCollect()).isEqualByComparingTo("4517.19");
    }

    @Test
    void noneKeepsGrossAsNet() {
        PayrollTaxes taxes = PayrollTaxCalculator.none(new BigDecimal("7000"));

        assertThat(taxes.net()).isEqualByComparingTo("7000");
        assertThat(taxes.taxesToCollect()).isEqualByComparingTo("0");
    }
}
