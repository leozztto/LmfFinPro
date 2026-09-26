package com.lmf.finpro.domain.model;

/** Se o cálculo desconta INSS e IRRF do pró-labore. */
public enum ProLaboreWithholdingMode {
    /** Liga para Simples Nacional e Lucro Presumido; MEI e autônomo não têm pró-labore formal. */
    AUTOMATIC,
    ENABLED,
    DISABLED;

    public boolean appliesTo(TaxRegime regime) {
        return switch (this) {
            case ENABLED -> true;
            case DISABLED -> false;
            case AUTOMATIC ->
                    regime == TaxRegime.SIMPLES_NACIONAL || regime == TaxRegime.LUCRO_PRESUMIDO;
        };
    }
}
