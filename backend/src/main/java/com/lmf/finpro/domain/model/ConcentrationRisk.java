package com.lmf.finpro.domain.model;

import java.math.BigDecimal;

/** Risco de dependência de um único cliente, pela participação dele na receita do período. */
public enum ConcentrationRisk {
    /** Sem receita no período. */
    NONE,
    LOW,
    MODERATE,
    HIGH;

    public static final BigDecimal MODERATE_THRESHOLD = new BigDecimal("0.30");
    public static final BigDecimal HIGH_THRESHOLD = new BigDecimal("0.50");

    /**
     * @param topClientShare fração da receita que vem do maior cliente (0 a 1)
     */
    public static ConcentrationRisk of(BigDecimal topClientShare, boolean hasIncome) {
        if (!hasIncome) {
            return NONE;
        }
        if (topClientShare.compareTo(HIGH_THRESHOLD) >= 0) {
            return HIGH;
        }
        if (topClientShare.compareTo(MODERATE_THRESHOLD) >= 0) {
            return MODERATE;
        }
        return LOW;
    }
}
