package com.lmf.finpro.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class TaxRegimeTest {

    @ParameterizedTest
    @EnumSource(
            value = TaxRegime.class,
            names = {"MEI", "SIMPLES_NACIONAL", "LUCRO_PRESUMIDO"})
    void legalEntityRegimesExpectCnpj(TaxRegime regime) {
        assertThat(regime.expectedDocumentType()).isEqualTo(DocumentType.CNPJ);
    }

    @ParameterizedTest
    @EnumSource(
            value = TaxRegime.class,
            names = {"AUTONOMO", "OUTRO"})
    void individualRegimesExpectCpf(TaxRegime regime) {
        assertThat(regime.expectedDocumentType()).isEqualTo(DocumentType.CPF);
    }
}
