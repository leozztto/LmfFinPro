package com.lmf.finpro.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.lmf.finpro.integration.support.CpfTestFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class CpfValidatorTest {

    @Test
    void acceptsRandomlyGeneratedValidCpf() {
        assertThat(CpfValidator.isValid(CpfTestFactory.randomValidCpf())).isTrue();
    }

    @Test
    void acceptsValidCpfWithFormattingCharacters() {
        assertThat(CpfValidator.isValid("529.982.247-25")).isTrue();
    }

    @Test
    void rejectsCpfWithWrongCheckDigits() {
        assertThat(CpfValidator.isValid("52998224700")).isFalse();
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "00000000000",
                "11111111111",
                "22222222222",
                "99999999999",
            })
    void rejectsCpfWithAllDigitsEqual(String cpf) {
        assertThat(CpfValidator.isValid(cpf)).isFalse();
    }

    @Test
    void rejectsCpfWithWrongLength() {
        assertThat(CpfValidator.isValid("123456789")).isFalse();
    }

    @ParameterizedTest
    @NullAndEmptySource
    void rejectsNullAndEmpty(String cpf) {
        assertThat(CpfValidator.isValid(cpf)).isFalse();
    }
}
