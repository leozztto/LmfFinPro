package com.lmf.finpro.infrastructure.pdf;

import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.TagTotalsReportData;
import com.lmf.finpro.domain.model.TransactionExportData;
import com.lmf.finpro.domain.model.TransactionReportData;
import com.lowagie.text.Element;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

/**
 * Relatórios em lista de lançamentos: extrato de transações, receitas/despesas e totais por tag.
 */
class TransactionListsPdf extends PdfReportSupport {

    TransactionListsPdf(Clock clock) {
        super(clock);
    }

    byte[] generateExport(TransactionExportData data) {
        return render(
                PageSize.A4.rotate(),
                "Falha ao gerar o PDF do extrato.",
                document -> {
                    String monthLabel = monthLabel(data.referenceMonth());

                    document.add(titleBlock("EXTRATO DE TRANSAÇÕES", monthLabel));
                    document.add(transactionExportBlock(data.rows()));
                    document.add(footer("extrato"));
                });
    }

    byte[] generateTransactionReport(TransactionReportData data) {
        boolean isIncome = data.type() == CategoryType.INCOME;
        return render(
                PageSize.A4.rotate(),
                "Falha ao gerar o PDF do relatório.",
                document -> {
                    document.add(
                            titleBlock(
                                    isIncome ? "RELATÓRIO DE RECEITAS" : "RELATÓRIO DE DESPESAS",
                                    data.periodLabel()));
                    document.add(appliedFiltersBlock(data.appliedFilters()));
                    document.add(transactionReportRowsBlock(data, isIncome));
                    document.add(transactionReportTotalsBlock(data, isIncome));
                    document.add(transactionReportCategoryBlock(data));
                    document.add(footer("relatório"));
                });
    }

    byte[] generateTagTotals(TagTotalsReportData data) {
        return render(
                PageSize.A4,
                "Falha ao gerar o PDF do relatório.",
                document -> {
                    document.add(titleBlock("TOTAIS POR TAG", data.periodLabel()));
                    document.add(appliedFiltersBlock(data.appliedFilters()));
                    document.add(tagTotalsBlock(data));
                    Paragraph note =
                            new Paragraph(
                                    "Uma transação com mais de uma tag entra no total de cada uma"
                                            + " delas, por isso as linhas não somam o total geral.",
                                    FOOTER_FONT);
                    note.setSpacingBefore(4);
                    document.add(note);
                    document.add(footer("relatório"));
                });
    }

    private PdfPTable tagTotalsBlock(TagTotalsReportData data) {
        PdfPTable table = gridTable(new float[] {2.4f, 1f, 1.4f, 1.4f, 1.4f});

        PdfPCell section = headerCell("RESULTADO POR TAG");
        section.setColspan(5);
        table.addCell(section);

        table.addCell(labelCell("Tag"));
        table.addCell(labelCell("Lançamentos"));
        for (String header : List.of("Receitas", "Despesas", "Resultado")) {
            PdfPCell cell = labelCell(header);
            cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(cell);
        }

        List<TagTotalsReportData.Row> rows = new ArrayList<>(data.rows());
        if (data.untagged() != null && data.untagged().count() > 0) {
            rows.add(data.untagged());
        }
        if (rows.isEmpty()) {
            PdfPCell empty = valueCell("Nenhuma transação com tag encontrada nesse período.");
            empty.setColspan(5);
            table.addCell(empty);
        } else {
            for (TagTotalsReportData.Row row : rows) {
                table.addCell(valueCell(row.label()));
                table.addCell(centeredValueCell(String.valueOf(row.count())));
                table.addCell(amountCell(formatCurrency(row.income()), BODY_FONT));
                table.addCell(amountCell(formatCurrency(row.expense()), BODY_FONT));
                table.addCell(amountCell(formatCurrency(row.result()), BODY_BOLD_FONT));
            }
        }
        return table;
    }

    private PdfPTable transactionReportRowsBlock(TransactionReportData data, boolean isIncome) {
        PdfPTable table = gridTable(new float[] {1f, 3f, 1.6f, 1.6f, 1.6f, 1f, 1.3f});

        PdfPCell section = headerCell(isIncome ? "RECEITAS" : "DESPESAS");
        section.setColspan(7);
        table.addCell(section);

        table.addCell(labelCell("Data"));
        table.addCell(labelCell("Descrição"));
        table.addCell(labelCell("Conta"));
        table.addCell(labelCell("Categoria"));
        table.addCell(labelCell("Cliente"));
        table.addCell(labelCell("Situação"));
        PdfPCell valueHeader = labelCell("Valor");
        valueHeader.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(valueHeader);

        if (data.rows().isEmpty()) {
            PdfPCell empty =
                    valueCell(
                            isIncome
                                    ? "Nenhuma receita encontrada com esses filtros."
                                    : "Nenhuma despesa encontrada com esses filtros.");
            empty.setColspan(7);
            table.addCell(empty);
        } else {
            for (TransactionReportData.Row row : data.rows()) {
                table.addCell(valueCell(row.date().format(DATE_FORMAT)));
                table.addCell(descriptionWithTagsCell(row.description(), row.tags()));
                table.addCell(valueCell(row.accountName()));
                table.addCell(valueCell(row.categoryName()));
                table.addCell(valueCell(row.clientName()));
                table.addCell(valueCell(statusLabel(row.status())));
                table.addCell(amountCell(formatCurrency(row.amount()), BODY_FONT));
            }
        }
        return table;
    }

    private PdfPTable transactionReportTotalsBlock(TransactionReportData data, boolean isIncome) {
        PdfPTable table = gridTable(new float[] {5f, 1.3f});
        table.addCell(labelCell("Total pago"));
        table.addCell(amountCell(formatCurrency(data.paidTotal()), BODY_FONT));
        table.addCell(labelCell("Total pendente"));
        table.addCell(amountCell(formatCurrency(data.pendingTotal()), BODY_FONT));
        table.addCell(headerCell(isIncome ? "TOTAL DE RECEITAS" : "TOTAL DE DESPESAS"));
        table.addCell(amountCell(formatCurrency(data.total()), TOTAL_FONT));
        return table;
    }

    private PdfPTable transactionReportCategoryBlock(TransactionReportData data) {
        PdfPTable table = gridTable(new float[] {3f, 1f, 1.3f});

        PdfPCell section = headerCell("SUBTOTAL POR CATEGORIA");
        section.setColspan(3);
        table.addCell(section);

        table.addCell(labelCell("Categoria"));
        table.addCell(labelCell("Lançamentos"));
        PdfPCell valueHeader = labelCell("Total");
        valueHeader.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(valueHeader);

        if (data.categoryTotals().isEmpty()) {
            PdfPCell empty = valueCell("—");
            empty.setColspan(3);
            table.addCell(empty);
        } else {
            for (TransactionReportData.CategoryTotal categoryTotal : data.categoryTotals()) {
                table.addCell(valueCell(categoryTotal.categoryName()));
                table.addCell(centeredValueCell(String.valueOf(categoryTotal.count())));
                table.addCell(amountCell(formatCurrency(categoryTotal.total()), BODY_FONT));
            }
        }
        return table;
    }

    /** Uma linha por transação do mês (em todas as contas, incluindo transferências), por data. */
    private PdfPTable transactionExportBlock(
            List<TransactionExportData.TransactionExportRow> rows) {
        PdfPTable table = gridTable(new float[] {1f, 1.6f, 1.6f, 1.8f, 3f, 1f, 1f, 1.3f});

        PdfPCell section = headerCell("TRANSAÇÕES DO PERÍODO");
        section.setColspan(8);
        table.addCell(section);

        table.addCell(labelCell("Data"));
        table.addCell(labelCell("Conta"));
        table.addCell(labelCell("Categoria"));
        table.addCell(labelCell("Cliente"));
        table.addCell(labelCell("Descrição"));
        table.addCell(labelCell("Tipo"));
        table.addCell(labelCell("Situação"));
        PdfPCell valueHeader = labelCell("Valor");
        valueHeader.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(valueHeader);

        if (rows.isEmpty()) {
            PdfPCell empty = valueCell("Nenhuma transação registrada neste período.");
            empty.setColspan(8);
            table.addCell(empty);
        } else {
            for (TransactionExportData.TransactionExportRow row : rows) {
                boolean isIncome = row.type() == CategoryType.INCOME;
                table.addCell(valueCell(row.date().format(DATE_FORMAT)));
                table.addCell(valueCell(row.accountName()));
                table.addCell(valueCell(row.categoryName()));
                table.addCell(valueCell(row.clientName()));
                table.addCell(descriptionWithTagsCell(row.description(), row.tags()));
                table.addCell(valueCell(isIncome ? "Receita" : "Despesa"));
                table.addCell(valueCell(statusLabel(row.status())));
                table.addCell(amountCell(formatCurrency(row.amount()), BODY_FONT));
            }
        }
        return table;
    }
}
