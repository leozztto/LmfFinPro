package com.lmf.finpro.application.importbatch;

import static org.assertj.core.api.Assertions.assertThat;

import com.lmf.finpro.domain.model.Category;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.Transaction;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class ImportSummaryTest {

    private static final Category FOOD =
            new Category(1L, 10L, "Alimentação", CategoryType.EXPENSE, null, null);
    private static final Category TRANSPORT =
            new Category(2L, 10L, "Transporte", CategoryType.EXPENSE, null, null);

    private static Transaction transaction(
            Long categoryId, String amount, LocalDate date, CategoryType type) {
        return Transaction.createImported(
                1L, categoryId, "descrição", new BigDecimal(amount), date, null, type, 7L);
    }

    @Test
    void totalsIncomeExpenseAndBalance() {
        ImportSummary summary =
                ImportSummary.of(
                        7L,
                        List.of(
                                transaction(
                                        null,
                                        "4200.00",
                                        LocalDate.of(2026, 1, 6),
                                        CategoryType.INCOME),
                                transaction(
                                        1L,
                                        "100.00",
                                        LocalDate.of(2026, 1, 5),
                                        CategoryType.EXPENSE),
                                transaction(
                                        2L,
                                        "50.00",
                                        LocalDate.of(2026, 1, 20),
                                        CategoryType.EXPENSE)),
                        List.of(FOOD, TRANSPORT));

        assertThat(summary.transactionCount()).isEqualTo(3);
        assertThat(summary.totalIncome()).isEqualByComparingTo("4200.00");
        assertThat(summary.totalExpense()).isEqualByComparingTo("150.00");
        assertThat(summary.balance()).isEqualByComparingTo("4050.00");
        assertThat(summary.firstDate()).isEqualTo(LocalDate.of(2026, 1, 5));
        assertThat(summary.lastDate()).isEqualTo(LocalDate.of(2026, 1, 20));
    }

    @Test
    void ranksExpenseCategoriesWithShareAndGroupsTheUncategorized() {
        ImportSummary summary =
                ImportSummary.of(
                        7L,
                        List.of(
                                transaction(
                                        1L,
                                        "75.00",
                                        LocalDate.of(2026, 1, 5),
                                        CategoryType.EXPENSE),
                                transaction(
                                        1L,
                                        "25.00",
                                        LocalDate.of(2026, 1, 6),
                                        CategoryType.EXPENSE),
                                transaction(
                                        null,
                                        "60.00",
                                        LocalDate.of(2026, 1, 7),
                                        CategoryType.EXPENSE),
                                transaction(
                                        2L,
                                        "50.00",
                                        LocalDate.of(2026, 1, 8),
                                        CategoryType.EXPENSE)),
                        List.of(FOOD, TRANSPORT));

        assertThat(summary.topExpenseCategories())
                .extracting(ImportSummary.CategoryTotal::name)
                .containsExactly("Alimentação", "Sem categoria", "Transporte");
        assertThat(summary.topExpenseCategories().get(0).share()).isEqualByComparingTo("47.6");
        assertThat(summary.uncategorizedCount()).isEqualTo(1);
    }

    @Test
    void keepsOnlyTheTopFiveCategories() {
        List<Category> categories =
                List.of(
                        category(1L),
                        category(2L),
                        category(3L),
                        category(4L),
                        category(5L),
                        category(6L));
        List<Transaction> transactions =
                categories.stream()
                        .map(
                                category ->
                                        transaction(
                                                category.id(),
                                                String.valueOf(category.id() * 10),
                                                LocalDate.of(2026, 1, 5),
                                                CategoryType.EXPENSE))
                        .toList();

        ImportSummary summary = ImportSummary.of(7L, transactions, categories);

        assertThat(summary.topExpenseCategories()).hasSize(ImportSummary.TOP_CATEGORIES);
        assertThat(summary.topExpenseCategories().get(0).categoryId()).isEqualTo(6L);
    }

    @Test
    void handlesAnEmptyBatch() {
        ImportSummary summary = ImportSummary.of(7L, List.of(), List.of());

        assertThat(summary.transactionCount()).isZero();
        assertThat(summary.balance()).isEqualByComparingTo("0");
        assertThat(summary.firstDate()).isNull();
        assertThat(summary.topExpenseCategories()).isEmpty();
    }

    private static Category category(long id) {
        return new Category(id, 10L, "Cat " + id, CategoryType.EXPENSE, null, null);
    }
}
