package com.lmf.finpro.domain.model;

import java.math.BigDecimal;

/**
 * Sugere uma alíquota de imposto por regime tributário, para pré-preencher a estimativa. Valores
 * simplificados e educacionais — não substituem orientação contábil.
 */
public final class TaxRateEstimator {

    private static final BigDecimal MEI_RATE = new BigDecimal("0.06");
    private static final BigDecimal SIMPLES_NACIONAL_RATE = new BigDecimal("0.06");
    private static final BigDecimal LUCRO_PRESUMIDO_RATE = new BigDecimal("0.1133");
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    // Tabela progressiva simplificada do IRPF mensal (carnê-leão), valores de referência 2024.
    private static final BigDecimal AUTONOMO_FAIXA_1 = new BigDecimal("2259.20");
    private static final BigDecimal AUTONOMO_FAIXA_2 = new BigDecimal("2826.65");
    private static final BigDecimal AUTONOMO_FAIXA_3 = new BigDecimal("3751.05");
    private static final BigDecimal AUTONOMO_FAIXA_4 = new BigDecimal("4664.68");
    private static final BigDecimal AUTONOMO_ALIQUOTA_2 = new BigDecimal("0.075");
    private static final BigDecimal AUTONOMO_ALIQUOTA_3 = new BigDecimal("0.15");
    private static final BigDecimal AUTONOMO_ALIQUOTA_4 = new BigDecimal("0.225");
    private static final BigDecimal AUTONOMO_ALIQUOTA_5 = new BigDecimal("0.275");

    private TaxRateEstimator() {}

    public static BigDecimal suggestRate(TaxRegime regime, BigDecimal grossRevenueMonth) {
        return switch (regime) {
            case MEI -> MEI_RATE;
            case SIMPLES_NACIONAL -> SIMPLES_NACIONAL_RATE;
            case LUCRO_PRESUMIDO -> LUCRO_PRESUMIDO_RATE;
            case AUTONOMO -> suggestAutonomoRate(grossRevenueMonth);
            case OUTRO -> ZERO;
        };
    }

    private static BigDecimal suggestAutonomoRate(BigDecimal grossRevenueMonth) {
        if (grossRevenueMonth.compareTo(AUTONOMO_FAIXA_1) <= 0) return ZERO;
        if (grossRevenueMonth.compareTo(AUTONOMO_FAIXA_2) <= 0) return AUTONOMO_ALIQUOTA_2;
        if (grossRevenueMonth.compareTo(AUTONOMO_FAIXA_3) <= 0) return AUTONOMO_ALIQUOTA_3;
        if (grossRevenueMonth.compareTo(AUTONOMO_FAIXA_4) <= 0) return AUTONOMO_ALIQUOTA_4;
        return AUTONOMO_ALIQUOTA_5;
    }
}
