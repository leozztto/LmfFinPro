package com.lmf.finpro.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class TaxRateEstimatorTest {

    @Test
    void meiAlwaysSuggestsSixPercent() {
        BigDecimal rate = TaxRateEstimator.suggestRate(TaxRegime.MEI, BigDecimal.valueOf(50000));
        assertThat(rate).isEqualByComparingTo("0.06");
    }

    @Test
    void simplesNacionalAlwaysSuggestsSixPercent() {
        BigDecimal rate =
                TaxRateEstimator.suggestRate(TaxRegime.SIMPLES_NACIONAL, BigDecimal.valueOf(5000));
        assertThat(rate).isEqualByComparingTo("0.06");
    }

    @Test
    void lucroPresumidoSuggestsElevenPointThreeThreePercent() {
        BigDecimal rate =
                TaxRateEstimator.suggestRate(TaxRegime.LUCRO_PRESUMIDO, BigDecimal.valueOf(10000));
        assertThat(rate).isEqualByComparingTo("0.1133");
    }

    @Test
    void outroAlwaysSuggestsZero() {
        BigDecimal rate = TaxRateEstimator.suggestRate(TaxRegime.OUTRO, BigDecimal.valueOf(99999));
        assertThat(rate).isEqualByComparingTo("0");
    }

    @Test
    void autonomoBelowFirstBracketSuggestsZero() {
        BigDecimal rate =
                TaxRateEstimator.suggestRate(TaxRegime.AUTONOMO, new BigDecimal("2259.20"));
        assertThat(rate).isEqualByComparingTo("0");
    }

    @Test
    void autonomoAtSecondBracketBoundarySuggestsSevenPointFivePercent() {
        BigDecimal rate =
                TaxRateEstimator.suggestRate(TaxRegime.AUTONOMO, new BigDecimal("2826.65"));
        assertThat(rate).isEqualByComparingTo("0.075");
    }

    @Test
    void autonomoAtThirdBracketBoundarySuggestsFifteenPercent() {
        BigDecimal rate =
                TaxRateEstimator.suggestRate(TaxRegime.AUTONOMO, new BigDecimal("3751.05"));
        assertThat(rate).isEqualByComparingTo("0.15");
    }

    @Test
    void autonomoAtFourthBracketBoundarySuggestsTwentyTwoPointFivePercent() {
        BigDecimal rate =
                TaxRateEstimator.suggestRate(TaxRegime.AUTONOMO, new BigDecimal("4664.68"));
        assertThat(rate).isEqualByComparingTo("0.225");
    }

    @Test
    void autonomoAboveHighestBracketSuggestsTwentySevenPointFivePercent() {
        BigDecimal rate =
                TaxRateEstimator.suggestRate(TaxRegime.AUTONOMO, BigDecimal.valueOf(10000));
        assertThat(rate).isEqualByComparingTo("0.275");
    }
}
