package com.lmf.finpro.infrastructure.pdf;

import com.lmf.finpro.domain.model.Currency;
import com.lmf.finpro.domain.model.DebtType;
import com.lmf.finpro.domain.model.NetWorthCalculator;
import com.lmf.finpro.domain.model.NetWorthReportData;
import com.lowagie.text.Element;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Clock;
import java.util.List;

/** Relatório de evolução patrimonial: contas, investimentos e dívidas mês a mês. */
class NetWorthPdf extends PdfReportSupport {

    NetWorthPdf(Clock clock) {
        super(clock);
    }

    byte[] generate(NetWorthReportData data) {
        return render(
                PageSize.A4.rotate(),
                "Falha ao gerar o PDF do relatório.",
                document -> {
                    NetWorthCalculator.Report report = data.report();
                    document.add(
                            titleBlock(
                                    "EVOLUÇÃO PATRIMONIAL",
                                    data.months() == 1
                                            ? "Mês atual"
                                            : "Últimos " + data.months() + " meses"));
                    document.add(partyBlock("TITULAR", issuerFields(data.issuer())));
                    document.add(netWorthSummaryBlock(report));
                    document.add(netWorthHistoryBlock(report.history()));
                    document.add(netWorthAccountsBlock(report.accounts()));
                    document.add(netWorthInvestmentsBlock(report.investments()));
                    document.add(netWorthDebtsBlock(report.debts()));
                    Paragraph note =
                            new Paragraph(
                                    "Patrimônio líquido = contas + investimentos (valor de mercado)"
                                            + " - dívidas (último saldo devedor informado). Valores em"
                                            + " outras moedas entram em reais pela cotação do fim de"
                                            + " cada mês.",
                                    FOOTER_FONT);
                    note.setSpacingBefore(4);
                    document.add(note);
                    document.add(footer("relatório"));
                });
    }

    /** Patrimônio atual e sua composição, com a variação sobre o mês anterior e o rendimento. */
    private PdfPTable netWorthSummaryBlock(NetWorthCalculator.Report report) {
        PdfPTable table = gridTable(new float[] {1f, 1f, 1f, 1f});

        table.addCell(headerCell("PATRIMÔNIO LÍQUIDO"));
        table.addCell(headerCell("CONTAS"));
        table.addCell(headerCell("INVESTIMENTOS"));
        table.addCell(headerCell("DÍVIDAS"));

        NetWorthCalculator.Point current = report.current();
        table.addCell(amountCell(formatCurrency(current.netWorth()), TOTAL_FONT));
        table.addCell(amountCell(formatCurrency(current.cash()), TOTAL_FONT));
        table.addCell(amountCell(formatCurrency(current.investments()), TOTAL_FONT));
        table.addCell(amountCell(formatCurrency(current.debts()), TOTAL_FONT));

        PdfPCell changeLabel = labelCell("Variação sobre o mês anterior");
        changeLabel.setColspan(2);
        table.addCell(changeLabel);
        PdfPCell gainLabel = labelCell("Rendimento dos investimentos");
        gainLabel.setColspan(2);
        table.addCell(gainLabel);

        PdfPCell change =
                amountCell(
                        report.changeFromPreviousMonth() == null
                                ? "—"
                                : formatCurrency(report.changeFromPreviousMonth()),
                        BODY_FONT);
        change.setColspan(2);
        table.addCell(change);
        PdfPCell gain = amountCell(formatCurrency(report.investmentGain()), BODY_FONT);
        gain.setColspan(2);
        table.addCell(gain);
        return table;
    }

    /**
     * Uma linha por mês, do mais antigo ao atual, com a variação do patrimônio sobre o anterior.
     */
    private PdfPTable netWorthHistoryBlock(List<NetWorthCalculator.Point> history) {
        PdfPTable table = gridTable(new float[] {1.8f, 1.3f, 1.3f, 1.3f, 1.4f, 1.3f});

        PdfPCell section = headerCell("EVOLUÇÃO MÊS A MÊS");
        section.setColspan(6);
        table.addCell(section);

        table.addCell(labelCell("Mês"));
        for (String header :
                List.of("Contas", "Investimentos", "Dívidas", "Patrimônio", "Variação")) {
            PdfPCell cell = labelCell(header);
            cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(cell);
        }

        BigDecimal previous = null;
        for (NetWorthCalculator.Point point : history) {
            table.addCell(valueCell(monthLabel(point.month())));
            table.addCell(amountCell(formatCurrency(point.cash()), BODY_FONT));
            table.addCell(amountCell(formatCurrency(point.investments()), BODY_FONT));
            table.addCell(amountCell(formatCurrency(point.debts()), BODY_FONT));
            table.addCell(amountCell(formatCurrency(point.netWorth()), BODY_BOLD_FONT));
            table.addCell(
                    amountCell(
                            previous == null
                                    ? "—"
                                    : formatCurrency(point.netWorth().subtract(previous)),
                            BODY_FONT));
            previous = point.netWorth();
        }
        return table;
    }

    private PdfPTable netWorthAccountsBlock(List<NetWorthCalculator.AccountRow> accounts) {
        PdfPTable table = gridTable(new float[] {3f, 1.5f, 1.5f});

        PdfPCell section = headerCell("CONTAS (SALDO ATUAL)");
        section.setColspan(3);
        table.addCell(section);

        table.addCell(labelCell("Conta"));
        table.addCell(rightLabelCell("Saldo"));
        table.addCell(rightLabelCell("Em reais"));

        if (accounts.isEmpty()) {
            PdfPCell empty = valueCell("Nenhuma conta cadastrada.");
            empty.setColspan(3);
            table.addCell(empty);
        } else {
            for (NetWorthCalculator.AccountRow row : accounts) {
                table.addCell(valueCell(row.account().name()));
                table.addCell(
                        amountCell(
                                formatCurrency(row.balance(), row.account().currency()),
                                BODY_FONT));
                table.addCell(amountCell(formatCurrency(row.balanceInBrl()), BODY_FONT));
            }
        }
        return table;
    }

    private PdfPTable netWorthInvestmentsBlock(List<NetWorthCalculator.InvestmentRow> investments) {
        PdfPTable table = gridTable(new float[] {2.4f, 1.3f, 1.3f, 1.3f, 0.9f, 1.3f, 1.2f});

        PdfPCell section = headerCell("INVESTIMENTOS");
        section.setColspan(7);
        table.addCell(section);

        table.addCell(labelCell("Investimento"));
        table.addCell(rightLabelCell("Aplicado"));
        table.addCell(rightLabelCell("Valor atual"));
        table.addCell(rightLabelCell("Rendimento"));
        table.addCell(rightLabelCell("%"));
        table.addCell(rightLabelCell("Valor em reais"));
        table.addCell(labelCell("Valor informado em"));

        if (investments.isEmpty()) {
            PdfPCell empty = valueCell("Nenhum investimento cadastrado.");
            empty.setColspan(7);
            table.addCell(empty);
        } else {
            for (NetWorthCalculator.InvestmentRow row : investments) {
                Currency currency = row.account().currency();
                table.addCell(valueCell(row.account().name()));
                table.addCell(amountCell(formatCurrency(row.invested(), currency), BODY_FONT));
                table.addCell(amountCell(formatCurrency(row.currentValue(), currency), BODY_FONT));
                table.addCell(amountCell(formatCurrency(row.gain(), currency), BODY_FONT));
                table.addCell(
                        amountCell(
                                row.gainRate() == null ? "—" : formatPercent(row.gainRate()),
                                BODY_FONT));
                table.addCell(amountCell(formatCurrency(row.currentValueInBrl()), BODY_FONT));
                table.addCell(
                        centeredValueCell(
                                row.lastValuation() == null
                                        ? "—"
                                        : row.lastValuation().valuationDate().format(DATE_FORMAT)));
            }
        }
        return table;
    }

    private PdfPTable netWorthDebtsBlock(List<NetWorthCalculator.DebtRow> debts) {
        PdfPTable table = gridTable(new float[] {2.4f, 1.3f, 1.8f, 1.4f, 1.2f});

        PdfPCell section = headerCell("DÍVIDAS");
        section.setColspan(5);
        table.addCell(section);

        table.addCell(labelCell("Dívida"));
        table.addCell(labelCell("Tipo"));
        table.addCell(labelCell("Credor"));
        table.addCell(rightLabelCell("Saldo devedor"));
        table.addCell(labelCell("Saldo informado em"));

        if (debts.isEmpty()) {
            PdfPCell empty = valueCell("Nenhuma dívida cadastrada.");
            empty.setColspan(5);
            table.addCell(empty);
        } else {
            for (NetWorthCalculator.DebtRow row : debts) {
                table.addCell(valueCell(row.debt().name()));
                table.addCell(valueCell(debtTypeLabel(row.debt().type())));
                table.addCell(valueCell(row.debt().creditor()));
                table.addCell(amountCell(formatCurrency(row.currentBalance()), BODY_FONT));
                table.addCell(
                        centeredValueCell(
                                row.lastBalance() == null
                                        ? "—"
                                        : row.lastBalance().balanceDate().format(DATE_FORMAT)));
            }
        }
        return table;
    }

    private String debtTypeLabel(DebtType type) {
        return switch (type) {
            case FINANCING -> "Financiamento";
            case LOAN -> "Empréstimo";
            case CREDIT_CARD -> "Cartão de crédito";
            case OTHER -> "Outra";
        };
    }

    private String formatPercent(BigDecimal rate) {
        NumberFormat format = NumberFormat.getPercentInstance(PT_BR);
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);
        return format.format(rate);
    }
}
