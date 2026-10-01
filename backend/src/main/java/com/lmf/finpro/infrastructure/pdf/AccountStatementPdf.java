package com.lmf.finpro.infrastructure.pdf;

import com.lmf.finpro.domain.model.Account;
import com.lmf.finpro.domain.model.AccountStatementData;
import com.lmf.finpro.domain.model.AccountType;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.Currency;
import com.lmf.finpro.domain.model.Transaction;
import com.lowagie.text.Element;
import com.lowagie.text.PageSize;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import java.math.BigDecimal;
import java.time.Clock;

/** Extrato de uma conta no mês, com saldo de abertura, movimentação e saldo final. */
class AccountStatementPdf extends PdfReportSupport {

    AccountStatementPdf(Clock clock) {
        super(clock);
    }

    byte[] generate(AccountStatementData data) {
        return render(
                PageSize.A4,
                "Falha ao gerar o PDF do extrato.",
                document -> {
                    String monthLabel = monthLabel(data.referenceMonth());

                    document.add(titleBlock("EXTRATO DE CONTA", monthLabel));
                    document.add(partyBlock("TITULAR", issuerFields(data.issuer())));
                    document.add(accountBlock(data.account(), data.openingBalance()));
                    document.add(statementTransactionsBlock(data));
                    document.add(
                            statementTotalsBlock(
                                    data.totalIncome(),
                                    data.totalExpense(),
                                    data.closingBalance(),
                                    data.account().currency()));
                    document.add(footer("extrato"));
                });
    }

    /** Nome/tipo da conta e o saldo de abertura do período (saldo inicial + tudo antes dele). */
    private PdfPTable accountBlock(Account account, BigDecimal openingBalance) {
        PdfPTable table = gridTable(new float[] {0.9f, 2.4f, 0.9f, 2.4f});

        PdfPCell header = headerCell("DADOS DA CONTA");
        header.setColspan(4);
        table.addCell(header);

        table.addCell(labelCell("Conta"));
        table.addCell(valueCell(account.name()));
        table.addCell(labelCell("Tipo"));
        table.addCell(valueCell(accountTypeLabel(account.type())));

        table.addCell(labelCell("Saldo de abertura"));
        table.addCell(valueCell(formatCurrency(openingBalance, account.currency())));
        table.completeRow();
        return table;
    }

    /** Movimentação do período com saldo corrente calculado linha a linha a partir da abertura. */
    private PdfPTable statementTransactionsBlock(AccountStatementData data) {
        PdfPTable table = gridTable(new float[] {1.1f, 3.2f, 1f, 1.2f, 1.3f});

        PdfPCell section = headerCell("MOVIMENTAÇÃO DO PERÍODO");
        section.setColspan(5);
        table.addCell(section);

        table.addCell(labelCell("Data"));
        table.addCell(labelCell("Descrição"));
        table.addCell(labelCell("Tipo"));
        PdfPCell valueHeader = labelCell("Valor");
        valueHeader.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(valueHeader);
        PdfPCell balanceHeader = labelCell("Saldo");
        balanceHeader.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(balanceHeader);

        if (data.transactions().isEmpty()) {
            PdfPCell empty = valueCell("Nenhuma movimentação registrada neste período.");
            empty.setColspan(5);
            table.addCell(empty);
        } else {
            Currency currency = data.account().currency();
            BigDecimal runningBalance = data.openingBalance();
            for (Transaction transaction : data.transactions()) {
                boolean isIncome = transaction.type() == CategoryType.INCOME;
                runningBalance =
                        isIncome
                                ? runningBalance.add(transaction.amount())
                                : runningBalance.subtract(transaction.amount());

                table.addCell(valueCell(transaction.transactionDate().format(DATE_FORMAT)));
                table.addCell(valueCell(transaction.description()));
                table.addCell(valueCell(isIncome ? "Receita" : "Despesa"));
                table.addCell(
                        amountCell(
                                (isIncome ? "+ " : "- ")
                                        + formatCurrency(transaction.amount(), currency),
                                BODY_FONT));
                table.addCell(amountCell(formatCurrency(runningBalance, currency), BODY_FONT));
            }
        }
        return table;
    }

    private PdfPTable statementTotalsBlock(
            BigDecimal totalIncome,
            BigDecimal totalExpense,
            BigDecimal closingBalance,
            Currency currency) {
        PdfPTable table = gridTable(new float[] {1f, 1f, 1f});

        table.addCell(headerCell("TOTAL DE RECEITAS"));
        table.addCell(headerCell("TOTAL DE DESPESAS"));
        table.addCell(headerCell("SALDO FINAL DO PERÍODO"));

        table.addCell(amountCell(formatCurrency(totalIncome, currency), TOTAL_FONT));
        table.addCell(amountCell(formatCurrency(totalExpense, currency), TOTAL_FONT));
        table.addCell(amountCell(formatCurrency(closingBalance, currency), TOTAL_FONT));
        return table;
    }

    private String accountTypeLabel(AccountType type) {
        return switch (type) {
            case CHECKING -> "Conta corrente";
            case SAVINGS -> "Poupança";
            case WALLET -> "Carteira";
            case INVESTMENT -> "Investimento";
            case RESERVE -> "Conta reserva";
        };
    }
}
