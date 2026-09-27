package com.lmf.finpro.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lmf.finpro.domain.exception.InvalidTagException;
import java.util.List;
import org.junit.jupiter.api.Test;

class TagTest {

    @Test
    void differentSpellingsOfTheSameTagNormalizeToOneName() {
        assertThat(Tag.normalizeName("#Site Acme")).isEqualTo("site-acme");
        assertThat(Tag.normalizeName("  SITE-ACME ")).isEqualTo("site-acme");
        assertThat(Tag.normalizeName("##site   acme")).isEqualTo("site-acme");
    }

    @Test
    void keepsAccentsDigitsAndUnderscore() {
        assertThat(Tag.normalizeName("#Dedutível")).isEqualTo("dedutível");
        assertThat(Tag.normalizeName("ir_2026")).isEqualTo("ir_2026");
    }

    @Test
    void rejectsEmptyTooLongAndInvalidNames() {
        assertThatThrownBy(() -> Tag.normalizeName(" # "))
                .isInstanceOf(InvalidTagException.class)
                .hasMessageContaining("Informe");
        assertThatThrownBy(() -> Tag.normalizeName("a".repeat(Tag.MAX_NAME_LENGTH + 1)))
                .isInstanceOf(InvalidTagException.class)
                .hasMessageContaining("40");
        assertThatThrownBy(() -> Tag.normalizeName("site/acme"))
                .isInstanceOf(InvalidTagException.class)
                .hasMessageContaining("caracteres inválidos");
    }

    @Test
    void colorIsOptionalButMustBeHex() {
        assertThat(Tag.create(1L, "a", null).color()).isNull();
        assertThat(Tag.create(1L, "a", " ").color()).isNull();
        assertThat(Tag.create(1L, "a", "#2AD6A5").color()).isEqualTo("#2ad6a5");
        assertThatThrownBy(() -> Tag.create(1L, "a", "verde"))
                .isInstanceOf(InvalidTagException.class);
    }

    @Test
    void joinsLabelsInAlphabeticalOrder() {
        assertThat(
                        Tag.joinLabels(
                                List.of(
                                        Tag.create(1L, "site-acme", null),
                                        Tag.create(1L, "b", null))))
                .isEqualTo("#b #site-acme");
        assertThat(Tag.joinLabels(List.of())).isEmpty();
    }
}
