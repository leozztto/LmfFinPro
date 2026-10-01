package com.lmf.finpro.infrastructure.pdf;

import com.lmf.finpro.domain.model.Address;
import com.lmf.finpro.domain.model.ClientAnnualStatementData;
import com.lmf.finpro.domain.model.ClientReceiptData;
import com.lmf.finpro.domain.model.Transaction;
import com.lmf.finpro.domain.model.User;
import com.lowagie.text.Chunk;
import com.lowagie.text.Element;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;

/** Documentos de um cliente: recibo mensal de prestação de serviços e demonstrativo anual. */
class ClientDocumentsPdf extends PdfReportSupport {

    ClientDocumentsPdf(Clock clock) {
        super(clock);
    }

    byte[] generateReceipt(ClientReceiptData data) {
        return render(
                PageSize.A4,
                "Falha ao gerar o PDF do recibo.",
                document -> {
                    String monthLabel = monthLabel(data.referenceMonth());

                    document.add(titleBlock("RECIBO DE PRESTAÇÃO DE SERVIÇOS", monthLabel));
                    document.add(partyBlock("PRESTADOR DO SERVIÇO", issuerFields(data.issuer())));
                    document.add(partyBlock("TOMADOR DO SERVIÇO", clientFields(data.client())));
                    document.add(declarationBlock(data.client().name(), monthLabel, data.total()));
                    document.add(transactionsBlock(data.transactions(), data.total()));
                    document.add(placeDateSignatureBlock(data.issuer()));
                    document.add(footer("recibo"));
                });
    }

    byte[] generateAnnualStatement(ClientAnnualStatementData data) {
        return render(
                PageSize.A4,
                "Falha ao gerar o PDF do demonstrativo.",
                document -> {
                    String yearLabel = data.referenceYear().toString();

                    document.add(titleBlock("DEMONSTRATIVO ANUAL DE RECEITA", yearLabel));
                    document.add(partyBlock("PRESTADOR", issuerFields(data.issuer())));
                    document.add(partyBlock("CLIENTE", clientFields(data.client())));
                    document.add(annualIncomeBlock(data.monthlyIncomes(), data.totalYear()));
                    document.add(footer("demonstrativo"));
                });
    }

    /** "Recebi de ... a importância de" à esquerda, com o valor total destacado à direita. */
    private PdfPTable declarationBlock(String clientName, String monthLabel, BigDecimal total) {
        PdfPTable table = gridTable(new float[] {4f, 1.3f, 1.3f});

        Paragraph text = new Paragraph();
        text.setLeading(14f);
        text.add(new Chunk("Recebi de ", BODY_FONT));
        text.add(new Chunk(clientName, BODY_BOLD_FONT));
        text.add(
                new Chunk(
                        ", acima identificado(a), a importância ao lado, pela prestação dos"
                                + " serviços especificados abaixo, referentes a "
                                + monthLabel
                                + ".",
                        BODY_FONT));
        PdfPCell textCell = new PdfPCell(text);
        textCell.setPadding(8);
        textCell.setBorderColor(BORDER_COLOR);
        table.addCell(textCell);

        PdfPCell label = headerCell("A IMPORTÂNCIA DE");
        label.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(label);

        table.addCell(amountCell(formatCurrency(total), TOTAL_FONT));
        return table;
    }

    private PdfPTable transactionsBlock(List<Transaction> transactions, BigDecimal total) {
        PdfPTable table = gridTable(new float[] {1.2f, 4.1f, 1.3f});

        PdfPCell section = headerCell("ESPECIFICAÇÃO DOS SERVIÇOS");
        section.setColspan(3);
        table.addCell(section);

        table.addCell(labelCell("Data"));
        table.addCell(labelCell("Descrição"));
        PdfPCell valueHeader = labelCell("Valor");
        valueHeader.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(valueHeader);

        if (transactions.isEmpty()) {
            PdfPCell empty = valueCell("Nenhuma receita registrada neste período.");
            empty.setColspan(3);
            table.addCell(empty);
        } else {
            for (Transaction transaction : transactions) {
                table.addCell(valueCell(transaction.transactionDate().format(DATE_FORMAT)));
                table.addCell(valueCell(transaction.description()));
                table.addCell(amountCell(formatCurrency(transaction.baseAmount()), BODY_FONT));
            }
        }

        PdfPCell totalLabel = headerCell("TOTAL RECEBIDO NO PERÍODO");
        totalLabel.setColspan(2);
        totalLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(totalLabel);
        table.addCell(amountCell(formatCurrency(total), TOTAL_FONT));
        return table;
    }

    /**
     * Uma linha por mês do ano (mesmo os zerados), na ordem de janeiro a dezembro, com o total
     * anual ao final.
     */
    private PdfPTable annualIncomeBlock(
            List<ClientAnnualStatementData.MonthlyIncome> monthlyIncomes, BigDecimal totalYear) {
        PdfPTable table = gridTable(new float[] {3f, 2f});

        PdfPCell section = headerCell("RECEITA POR MÊS");
        section.setColspan(2);
        table.addCell(section);

        table.addCell(labelCell("Mês"));
        PdfPCell valueHeader = labelCell("Valor recebido");
        valueHeader.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(valueHeader);

        for (ClientAnnualStatementData.MonthlyIncome monthlyIncome : monthlyIncomes) {
            table.addCell(valueCell(monthLabelOnly(monthlyIncome.month())));
            table.addCell(amountCell(formatCurrency(monthlyIncome.total()), BODY_FONT));
        }

        PdfPCell totalLabel = headerCell("TOTAL RECEBIDO NO ANO");
        table.addCell(totalLabel);
        table.addCell(amountCell(formatCurrency(totalYear), TOTAL_FONT));
        return table;
    }

    /** Linha final "Localidade | Data | Assinatura", com espaço em branco para assinar à mão. */
    private PdfPTable placeDateSignatureBlock(User issuer) {
        PdfPTable table = gridTable(new float[] {2f, 1.2f, 3.4f});

        table.addCell(headerCell("LOCALIDADE"));
        table.addCell(headerCell("DATA"));
        table.addCell(headerCell("ASSINATURA DO PRESTADOR"));

        Address address = issuer.address();
        PdfPCell placeCell =
                centeredValueCell(address == null ? "" : address.city() + "/" + address.state());
        placeCell.setMinimumHeight(52);
        table.addCell(placeCell);

        table.addCell(centeredValueCell("____/____/______"));

        PdfPCell signature = new PdfPCell(new Paragraph(issuer.name(), LABEL_FONT));
        signature.setHorizontalAlignment(Element.ALIGN_CENTER);
        signature.setVerticalAlignment(Element.ALIGN_BOTTOM);
        signature.setPaddingBottom(4);
        signature.setBorderColor(BORDER_COLOR);
        table.addCell(signature);
        return table;
    }
}
