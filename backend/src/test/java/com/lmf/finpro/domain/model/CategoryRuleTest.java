package com.lmf.finpro.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CategoryRuleTest {

    @Test
    void newRuleStartsWithWeightOne() {
        CategoryRule rule = CategoryRule.create(1L, "UBER", 10L);

        assertThat(rule.weight()).isEqualTo(1);
        assertThat(rule.categoryId()).isEqualTo(10L);
    }

    @Test
    void matchesIsCaseInsensitiveAndLooksForSubstring() {
        CategoryRule rule = CategoryRule.create(1L, "uber", 10L);

        assertThat(rule.matches("PAGAMENTO UBER TRIP")).isTrue();
        assertThat(rule.matches("pagamento uber trip")).isTrue();
        assertThat(rule.matches("99 TAXI")).isFalse();
    }

    @Test
    void matchesReturnsFalseForNullDescription() {
        CategoryRule rule = CategoryRule.create(1L, "UBER", 10L);

        assertThat(rule.matches(null)).isFalse();
    }

    @Test
    void reinforcedWithSameCategoryIncreasesWeight() {
        CategoryRule rule = new CategoryRule(1L, 1L, "UBER", 10L, 3);

        CategoryRule reinforced = rule.reinforcedWith(10L);

        assertThat(reinforced.weight()).isEqualTo(4);
        assertThat(reinforced.categoryId()).isEqualTo(10L);
    }

    @Test
    void reinforcedWithDifferentCategoryResetsWeightToOne() {
        CategoryRule rule = new CategoryRule(1L, 1L, "UBER", 10L, 5);

        CategoryRule corrected = rule.reinforcedWith(20L);

        assertThat(corrected.weight()).isEqualTo(1);
        assertThat(corrected.categoryId()).isEqualTo(20L);
    }

    @Test
    void withDetailsReplacesPatternAndCategoryButKeepsWeight() {
        CategoryRule rule = new CategoryRule(1L, 1L, "UBER", 10L, 7);

        CategoryRule updated = rule.withDetails("99POP", 20L);

        assertThat(updated.pattern()).isEqualTo("99POP");
        assertThat(updated.categoryId()).isEqualTo(20L);
        assertThat(updated.weight()).isEqualTo(7);
    }
}
