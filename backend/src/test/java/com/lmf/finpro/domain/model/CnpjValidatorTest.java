package com.lmf.finpro.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.lmf.finpro.integration.support.CnpjTestFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

class CnpjValidatorTest {

    @Test
    void acceptsRandomlyGeneratedValidCnpj() {
        assertThat(CnpjValidator.isValid(CnpjTestFactory.randomValidCnpj())).isTrue();
    }

    @Test
    void acceptsValidCnpjWithFormattingCharacters() {
        assertThat(CnpjValidator.isValid("11.444.777/0001-61")).isTrue();
    }

    @Test
    void rejectsCnpjWithWrongCheckDigits() {
        assertThat(CnpjValidator.isValid("11444777000100")).isFalse();
    }

    @Test
    void rejectsCnpjWithAllDigitsEqual() {
        assertThat(CnpjValidator.isValid("11111111111111")).isFalse();
    }

    @Test
    void rejectsCnpjWithWrongLength() {
        assertThat(CnpjValidator.isValid("123456789")).isFalse();
    }

    @ParameterizedTest
    @NullAndEmptySource
    void rejectsNullAndEmpty(String cnpj) {
        assertThat(CnpjValidator.isValid(cnpj)).isFalse();
    }
}
