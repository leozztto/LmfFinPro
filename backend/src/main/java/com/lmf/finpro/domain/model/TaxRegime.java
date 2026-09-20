package com.lmf.finpro.domain.model;

/** Regime tributário do usuário para fins de estimativa de imposto — espelha as opções oferecidas no cadastro. */
public enum TaxRegime {
    AUTONOMO,
    MEI,
    SIMPLES_NACIONAL,
    LUCRO_PRESUMIDO,
    OUTRO
}
