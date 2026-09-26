package com.lmf.finpro.domain.model;

import java.math.BigDecimal;

/**
 * Configuração do cálculo de pró-labore.
 *
 * @param cashCushionMonths meses da despesa média PJ mantidos em caixa (base {@code
 *     CURRENT_BALANCE})
 * @param reserveRate fração da receita do mês retida na empresa (base {@code MONTH_INCOME}), ex.:
 *     0.10 = 10%
 * @param manualTaxRate alíquota usada quando {@code taxMode} é {@code MANUAL}
 * @param fixedAmount pró-labore fixo mensal (bruto), opcional
 * @param withholdingMode se desconta INSS e IRRF do pró-labore
 * @param employerInssRate INSS patronal sobre o pró-labore; null = automático pelo regime
 */
public record ProLaboreSettings(
        Long userId,
        ProLaboreCalculationBase calculationBase,
        int cashCushionMonths,
        BigDecimal reserveRate,
        ProLaboreTaxMode taxMode,
        BigDecimal manualTaxRate,
        BigDecimal fixedAmount,
        ProLaboreWithholdingMode withholdingMode,
        BigDecimal employerInssRate) {

    public static final int DEFAULT_CASH_CUSHION_MONTHS = 1;
    public static final int MAX_CASH_CUSHION_MONTHS = 12;
    public static final BigDecimal DEFAULT_RESERVE_RATE = new BigDecimal("0.10");
    public static final BigDecimal LUCRO_PRESUMIDO_EMPLOYER_INSS_RATE = new BigDecimal("0.20");

    public ProLaboreSettings {
        if (taxMode == ProLaboreTaxMode.MANUAL && manualTaxRate == null) {
            throw new IllegalArgumentException("Informe a alíquota do imposto no modo manual");
        }
        if (fixedAmount != null && fixedAmount.signum() <= 0) {
            fixedAmount = null;
        }
        if (withholdingMode == null) {
            withholdingMode = ProLaboreWithholdingMode.AUTOMATIC;
        }
    }

    public static ProLaboreSettings defaults(Long userId) {
        return new ProLaboreSettings(
                userId,
                ProLaboreCalculationBase.MONTH_INCOME,
                DEFAULT_CASH_CUSHION_MONTHS,
                DEFAULT_RESERVE_RATE,
                ProLaboreTaxMode.AUTOMATIC,
                null,
                null,
                ProLaboreWithholdingMode.AUTOMATIC,
                null);
    }

    public boolean withholdingAppliesTo(TaxRegime regime) {
        return withholdingMode.appliesTo(regime);
    }

    /**
     * INSS patronal efetivo: o informado ou, no automático, 20% no Lucro Presumido e 0% nos demais
     * (no Simples a contribuição patronal vai no DAS na maioria dos anexos). Sem retenção, zero.
     */
    public BigDecimal effectiveEmployerInssRate(TaxRegime regime) {
        if (!withholdingAppliesTo(regime)) {
            return BigDecimal.ZERO;
        }
        if (employerInssRate != null) {
            return employerInssRate;
        }
        return regime == TaxRegime.LUCRO_PRESUMIDO
                ? LUCRO_PRESUMIDO_EMPLOYER_INSS_RATE
                : BigDecimal.ZERO;
    }
}
