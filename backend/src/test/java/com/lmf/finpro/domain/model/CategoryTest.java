package com.lmf.finpro.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CategoryTest {

    @Test
    void categoryWithNullUserIdIsGlobal() {
        Category category = Category.create(null, "Alimentação", CategoryType.EXPENSE, null, null);

        assertThat(category.isGlobal()).isTrue();
        assertThat(category.isOwnedBy(1L)).isFalse();
    }

    @Test
    void categoryWithUserIdIsOwnedByThatUserOnly() {
        Category category = Category.create(1L, "Consultoria", CategoryType.INCOME, null, null);

        assertThat(category.isGlobal()).isFalse();
        assertThat(category.isOwnedBy(1L)).isTrue();
        assertThat(category.isOwnedBy(2L)).isFalse();
    }

    @Test
    void globalCategoryIsVisibleToEveryUser() {
        Category category = Category.create(null, "Transporte", CategoryType.EXPENSE, null, null);

        assertThat(category.isVisibleTo(1L)).isTrue();
        assertThat(category.isVisibleTo(999L)).isTrue();
    }

    @Test
    void ownedCategoryIsOnlyVisibleToItsOwner() {
        Category category = Category.create(1L, "Consultoria", CategoryType.INCOME, null, null);

        assertThat(category.isVisibleTo(1L)).isTrue();
        assertThat(category.isVisibleTo(2L)).isFalse();
    }

    @Test
    void withDetailsReplacesFieldsButKeepsIdAndUserId() {
        Category category =
                new Category(5L, 1L, "Antigo", CategoryType.EXPENSE, "#000", "old-icon");

        Category updated = category.withDetails("Novo", CategoryType.EXPENSE, "#fff", "new-icon");

        assertThat(updated.id()).isEqualTo(5L);
        assertThat(updated.userId()).isEqualTo(1L);
        assertThat(updated.name()).isEqualTo("Novo");
        assertThat(updated.color()).isEqualTo("#fff");
        assertThat(updated.icon()).isEqualTo("new-icon");
    }
}
