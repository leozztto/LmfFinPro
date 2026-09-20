package com.lmf.finpro.domain.model;

/**
 * Regime tributário do usuário para fins de estimativa de imposto — espelha as opções oferecidas no
 * cadastro.
 */
public enum TaxRegime {
    AUTONOMO,
    MEI,
    SIMPLES_NACIONAL,
    LUCRO_PRESUMIDO,
    OUTRO;

    /**
     * MEI, Simples Nacional e Lucro Presumido são regimes de pessoa jurídica — exigem CNPJ; os
     * demais, CPF.
     */
    public DocumentType expectedDocumentType() {
        return switch (this) {
            case MEI, SIMPLES_NACIONAL, LUCRO_PRESUMIDO -> DocumentType.CNPJ;
            case AUTONOMO, OUTRO -> DocumentType.CPF;
        };
    }
}
