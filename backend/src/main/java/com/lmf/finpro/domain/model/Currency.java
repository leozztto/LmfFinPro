package com.lmf.finpro.domain.model;

/**
 * Moedas aceitas nas contas e nas transações. O BRL é a moeda de consolidação: todo total que junta
 * contas diferentes (dashboard, relatórios, patrimônio) é mostrado em reais.
 */
public enum Currency {
    BRL,
    USD,
    EUR;

    public boolean isBase() {
        return this == BRL;
    }
}
