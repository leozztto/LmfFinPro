package com.lmf.finpro.infrastructure.web.dto.importbatch;

import com.lmf.finpro.application.importbatch.ImportSummary;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ImportSummaryResponse(
        Long batchId,
        int transactionCount,
        LocalDate firstDate,
        LocalDate lastDate,
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal balance,
        List<CategoryTotalResponse> topExpenseCategories,
        int uncategorizedCount) {

    public record CategoryTotalResponse(
            Long categoryId, String name, BigDecimal total, BigDecimal share) {}

    public static ImportSummaryResponse from(ImportSummary summary) {
        return new ImportSummaryResponse(
                summary.batchId(),
                summary.transactionCount(),
                summary.firstDate(),
                summary.lastDate(),
                summary.totalIncome(),
                summary.totalExpense(),
                summary.balance(),
                summary.topExpenseCategories().stream()
                        .map(
                                category ->
                                        new CategoryTotalResponse(
                                                category.categoryId(),
                                                category.name(),
                                                category.total(),
                                                category.share()))
                        .toList(),
                summary.uncategorizedCount());
    }
}
