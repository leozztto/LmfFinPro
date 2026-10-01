package com.lmf.finpro.infrastructure.pdf;

import com.lmf.finpro.domain.model.BudgetVsActualReportData;
import com.lmf.finpro.domain.model.CategoryExpenseReportData;
import com.lmf.finpro.domain.model.IncomeStatementData;
import com.lmf.finpro.domain.model.ReportGranularity;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;

/** Relatórios consolidados do usuário: despesas por categoria, DRE e orçamento vs. realizado. */
class PeriodReportsPdf extends PdfReportSupport {

    PeriodReportsPdf(Clock clock) {
        super(clock);
    }

    byte[] generateCategoryExpense(CategoryExpenseReportData data) {
        return render(
                PageSize.A4,
                "Falha ao gerar o PDF do relatório.",
                document -> {
                    String monthLabel = monthLabel(data.referenceMonth());

                    document.add(titleBlock("DESPESAS POR CATEGORIA", monthLabel));
                    document.add(partyBlock("TITULAR", issuerFields(data.issuer())));
                    document.add(
                            categoryExpensesBlock(data.categoryExpenses(), data.totalExpense()));
                    document.add(footer("relatório"));
                });
    }

    byte[] generateIncomeStatement(IncomeStatementData data) {
        return render(
                PageSize.A4,
                "Falha ao gerar o PDF do relatório.",
                document -> {
                    String referenceLabel =
                            data.referenceYear()
                                    + " ("
                                    + granularityLabel(data.granularity())
                                    + ")";

                    document.add(titleBlock("RESULTADO DO PERÍODO (DRE)", referenceLabel));
                    document.add(partyBlock("TITULAR", issuerFields(data.issuer())));
                    document.add(
                            incomeStatementBlock(
                                    data.periods(),
                                    data.totalIncome(),
                                    data.totalExpense(),
                                    data.totalResult()));
                    document.add(footer("relatório"));
                });
    }

    byte[] generateBudgetVsActual(BudgetVsActualReportData data) {
        return render(
                PageSize.A4,
                "Falha ao gerar o PDF do relatório.",
                document -> {
                    String monthLabel = monthLabel(data.referenceMonth());

                    document.add(titleBlock("ORÇAMENTO VS. REALIZADO", monthLabel));
                    document.add(partyBlock("TITULAR", issuerFields(data.issuer())));
                    document.add(
                            budgetVsActualBlock(
                                    data.comparisons(), data.totalLimit(), data.totalSpent()));
                    document.add(footer("relatório"));
                });
    }

    /** Uma linha por categoria com despesa no período (já vem ordenada da maior para a menor). */
    private PdfPTable categoryExpensesBlock(
            List<CategoryExpenseReportData.CategoryExpense> categoryExpenses,
            BigDecimal totalExpense) {
        PdfPTable table = gridTable(new float[] {3f, 2f});

        PdfPCell section = headerCell("DESPESAS POR CATEGORIA");
        section.setColspan(2);
        table.addCell(section);

        table.addCell(labelCell("Categoria"));
        PdfPCell valueHeader = labelCell("Valor gasto");
        valueHeader.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(valueHeader);

        if (categoryExpenses.isEmpty()) {
            PdfPCell empty = valueCell("Nenhuma despesa registrada neste período.");
            empty.setColspan(2);
            table.addCell(empty);
        } else {
            for (CategoryExpenseReportData.CategoryExpense categoryExpense : categoryExpenses) {
                table.addCell(valueCell(categoryExpense.categoryName()));
                table.addCell(amountCell(formatCurrency(categoryExpense.total()), BODY_FONT));
            }
        }

        PdfPCell totalLabel = headerCell("TOTAL DE DESPESAS NO PERÍODO");
        table.addCell(totalLabel);
        table.addCell(amountCell(formatCurrency(totalExpense), TOTAL_FONT));
        return table;
    }

    /** Uma linha por período (mês, trimestre ou ano) com receita, despesa e resultado. */
    private PdfPTable incomeStatementBlock(
            List<IncomeStatementData.PeriodResult> periods,
            BigDecimal totalIncome,
            BigDecimal totalExpense,
            BigDecimal totalResult) {
        PdfPTable table = gridTable(new float[] {1.6f, 1.3f, 1.3f, 1.3f});

        PdfPCell section = headerCell("RESULTADO POR PERÍODO");
        section.setColspan(4);
        table.addCell(section);

        table.addCell(labelCell("Período"));
        PdfPCell incomeHeader = labelCell("Receita");
        incomeHeader.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(incomeHeader);
        PdfPCell expenseHeader = labelCell("Despesa");
        expenseHeader.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(expenseHeader);
        PdfPCell resultHeader = labelCell("Resultado");
        resultHeader.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(resultHeader);

        for (IncomeStatementData.PeriodResult period : periods) {
            table.addCell(valueCell(period.label()));
            table.addCell(amountCell(formatCurrency(period.income()), BODY_FONT));
            table.addCell(amountCell(formatCurrency(period.expense()), BODY_FONT));
            table.addCell(amountCell(formatCurrency(period.result()), BODY_FONT));
        }

        PdfPCell totalLabel = headerCell("TOTAL DO PERÍODO");
        table.addCell(totalLabel);
        table.addCell(amountCell(formatCurrency(totalIncome), TOTAL_FONT));
        table.addCell(amountCell(formatCurrency(totalExpense), TOTAL_FONT));
        table.addCell(amountCell(formatCurrency(totalResult), TOTAL_FONT));
        return table;
    }

    /**
     * Uma linha por orçamento cadastrado no mês (já vem ordenada do maior percentual de uso para o
     * menor), com o limite, o gasto real e a diferença — em vermelho quando o limite foi estourado.
     */
    private PdfPTable budgetVsActualBlock(
            List<BudgetVsActualReportData.BudgetComparison> comparisons,
            BigDecimal totalLimit,
            BigDecimal totalSpent) {
        PdfPTable table = gridTable(new float[] {1.6f, 1.3f, 1.3f, 1.3f});

        PdfPCell section = headerCell("ORÇAMENTO POR CATEGORIA");
        section.setColspan(4);
        table.addCell(section);

        table.addCell(labelCell("Categoria"));
        PdfPCell limitHeader = labelCell("Limite");
        limitHeader.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(limitHeader);
        PdfPCell spentHeader = labelCell("Gasto real");
        spentHeader.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(spentHeader);
        PdfPCell differenceHeader = labelCell("Diferença");
        differenceHeader.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(differenceHeader);

        if (comparisons.isEmpty()) {
            PdfPCell empty = valueCell("Nenhum orçamento cadastrado neste mês.");
            empty.setColspan(4);
            table.addCell(empty);
        } else {
            for (BudgetVsActualReportData.BudgetComparison comparison : comparisons) {
                Font font = comparison.exceeded() ? EXCEEDED_FONT : BODY_FONT;
                table.addCell(valueCell(comparison.categoryName()));
                table.addCell(amountCell(formatCurrency(comparison.limitValue()), BODY_FONT));
                table.addCell(amountCell(formatCurrency(comparison.spentValue()), font));
                table.addCell(amountCell(formatCurrency(comparison.difference()), font));
            }
        }

        PdfPCell totalLabel = headerCell("TOTAL DO MÊS");
        table.addCell(totalLabel);
        table.addCell(amountCell(formatCurrency(totalLimit), TOTAL_FONT));
        table.addCell(amountCell(formatCurrency(totalSpent), TOTAL_FONT));
        table.addCell(amountCell(formatCurrency(totalLimit.subtract(totalSpent)), TOTAL_FONT));
        return table;
    }

    private String granularityLabel(ReportGranularity granularity) {
        return switch (granularity) {
            case MONTHLY -> "Mensal";
            case QUARTERLY -> "Trimestral";
            case YEARLY -> "Anual";
        };
    }
}
