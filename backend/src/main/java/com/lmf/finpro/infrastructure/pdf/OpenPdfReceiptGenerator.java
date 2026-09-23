package com.lmf.finpro.infrastructure.pdf;

import com.lmf.finpro.domain.model.Address;
import com.lmf.finpro.domain.model.Client;
import com.lmf.finpro.domain.model.ClientReceiptData;
import com.lmf.finpro.domain.model.DocumentType;
import com.lmf.finpro.domain.model.Transaction;
import com.lmf.finpro.domain.model.User;
import com.lmf.finpro.domain.port.out.ReceiptGeneratorPort;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Monta o PDF do recibo com a biblioteca OpenPDF, seguindo a mesma filosofia "sem framework de
 * template" já usada no resto do backend (ex.: {@code CsvTransactionParser} monta CSV na mão).
 *
 * <p>O layout imita os formulários de recibo impressos (estilo RPA): uma única grade contínua com
 * cabeçalhos em azul-claro, formada por tabelas de largura total empilhadas sem espaço entre si.
 */
@Component
public class OpenPdfReceiptGenerator implements ReceiptGeneratorPort {

    private static final Locale PT_BR = Locale.of("pt", "BR");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final Color HEADER_BACKGROUND = new Color(214, 230, 247);
    private static final Color BORDER_COLOR = new Color(120, 150, 185);

    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15);
    private static final Font HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);
    private static final Font LABEL_FONT = FontFactory.getFont(FontFactory.HELVETICA, 8);
    private static final Font BODY_FONT = FontFactory.getFont(FontFactory.HELVETICA, 10);
    private static final Font BODY_BOLD_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
    private static final Font TOTAL_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
    private static final Font FOOTER_FONT = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 7);

    @Override
    public byte[] generateClientReceipt(ClientReceiptData data) {
        Document document = new Document(PageSize.A4, 36, 36, 40, 36);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(document, output);
            document.open();

            String monthLabel =
                    capitalize(
                                    data.referenceMonth()
                                            .getMonth()
                                            .getDisplayName(TextStyle.FULL, PT_BR))
                            + " de "
                            + data.referenceMonth().getYear();

            document.add(titleBlock(monthLabel));
            document.add(partyBlock("PRESTADOR DO SERVIÇO", issuerFields(data.issuer())));
            document.add(partyBlock("TOMADOR DO SERVIÇO", clientFields(data.client())));
            document.add(declarationBlock(data.client().name(), monthLabel, data.total()));
            document.add(transactionsBlock(data.transactions(), data.total()));
            document.add(placeDateSignatureBlock(data.issuer()));

            Paragraph footer =
                    new Paragraph(
                            "Documento gerado automaticamente pelo FinPro em "
                                    + LocalDateTime.now().format(TIMESTAMP_FORMAT)
                                    + ". Não possui validade fiscal.",
                            FOOTER_FONT);
            footer.setSpacingBefore(6);
            document.add(footer);
        } catch (DocumentException e) {
            throw new IllegalStateException("Falha ao gerar o PDF do recibo.", e);
        } finally {
            document.close();
        }
        return output.toByteArray();
    }

    /** Título à esquerda; à direita, os cabeçalhos "Referência" e "Emissão" com os valores abaixo. */
    private PdfPTable titleBlock(String monthLabel) {
        PdfPTable table = gridTable(new float[] {4f, 1.3f, 1.3f});

        PdfPCell title =
                new PdfPCell(new Paragraph("RECIBO DE PRESTAÇÃO DE SERVIÇOS", TITLE_FONT));
        title.setRowspan(2);
        title.setVerticalAlignment(Element.ALIGN_MIDDLE);
        title.setPadding(10);
        title.setBorderColor(BORDER_COLOR);
        table.addCell(title);

        table.addCell(headerCell("REFERÊNCIA"));
        table.addCell(headerCell("EMISSÃO"));
        table.addCell(centeredValueCell(monthLabel));
        table.addCell(centeredValueCell(LocalDateTime.now().format(DATE_FORMAT)));
        return table;
    }

    /**
     * Faixa de cabeçalho com o nome da parte e, abaixo, pares "rótulo | valor" em duas colunas. Um
     * campo com terceiro elemento (ex.: Endereço) é tratado como largo e ocupa a linha inteira.
     */
    private PdfPTable partyBlock(String title, String[][] fields) {
        PdfPTable table = gridTable(new float[] {0.9f, 2.4f, 0.9f, 2.4f});

        PdfPCell header = headerCell(title);
        header.setColspan(4);
        table.addCell(header);

        for (String[] field : fields) {
            table.addCell(labelCell(field[0]));
            PdfPCell value = valueCell(field[1]);
            if (field.length > 2) {
                value.setColspan(3);
            }
            table.addCell(value);
        }
        table.completeRow();
        return table;
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
                table.addCell(amountCell(formatCurrency(transaction.amount()), BODY_FONT));
            }
        }

        PdfPCell totalLabel = headerCell("TOTAL RECEBIDO NO PERÍODO");
        totalLabel.setColspan(2);
        totalLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(totalLabel);
        table.addCell(amountCell(formatCurrency(total), TOTAL_FONT));
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

    private String[][] issuerFields(User issuer) {
        return new String[][] {
            {"Nome", issuer.name()},
            {
                documentLabel(issuer.documentType()),
                formatDocument(issuer.documentType(), issuer.documentNumber())
            },
            {"Telefone", formatPhone(issuer.phone())},
            {"E-mail", issuer.email()},
            {"Endereço", formatAddress(issuer.address()), "largo"},
        };
    }

    private String[][] clientFields(Client client) {
        return new String[][] {
            {"Nome", client.name()},
            {
                documentLabel(client.documentType()),
                formatDocument(client.documentType(), client.documentNumber())
            },
            {"Telefone", formatPhone(client.phone())},
            {
                "E-mail",
                client.email() == null || client.email().isBlank()
                        ? "Não informado"
                        : client.email()
            },
        };
    }

    private PdfPTable gridTable(float[] widths) {
        PdfPTable table = new PdfPTable(widths);
        table.setWidthPercentage(100);
        table.setSpacingBefore(0);
        table.setSpacingAfter(0);
        return table;
    }

    private PdfPCell headerCell(String text) {
        PdfPCell cell = new PdfPCell(new Paragraph(text, HEADER_FONT));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBackgroundColor(HEADER_BACKGROUND);
        cell.setBorderColor(BORDER_COLOR);
        cell.setPadding(4);
        return cell;
    }

    private PdfPCell labelCell(String text) {
        PdfPCell cell = new PdfPCell(new Paragraph(text.toUpperCase(PT_BR), LABEL_FONT));
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBackgroundColor(HEADER_BACKGROUND);
        cell.setBorderColor(BORDER_COLOR);
        cell.setPadding(4);
        return cell;
    }

    private PdfPCell valueCell(String text) {
        PdfPCell cell = new PdfPCell(new Paragraph(text == null ? "" : text, BODY_FONT));
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBorderColor(BORDER_COLOR);
        cell.setPadding(5);
        return cell;
    }

    private PdfPCell centeredValueCell(String text) {
        PdfPCell cell = valueCell(text);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        return cell;
    }

    private PdfPCell amountCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Paragraph(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBorderColor(BORDER_COLOR);
        cell.setPadding(6);
        return cell;
    }

    private String documentLabel(DocumentType type) {
        return type == null ? "Documento" : type.name();
    }

    private String formatAddress(Address address) {
        if (address == null) {
            return "Endereço não informado";
        }
        String complement =
                address.complement() == null || address.complement().isBlank()
                        ? ""
                        : " - " + address.complement();
        return "%s, %s%s - %s, %s/%s - CEP %s"
                .formatted(
                        address.street(),
                        address.number(),
                        complement,
                        address.neighborhood(),
                        address.city(),
                        address.state(),
                        address.zipCode());
    }

    private String formatDocument(DocumentType type, String number) {
        if (number == null) {
            return "";
        }
        String digits = number.replaceAll("\\D", "");
        if (type == DocumentType.CPF && digits.length() == 11) {
            return digits.replaceFirst("(\\d{3})(\\d{3})(\\d{3})(\\d{2})", "$1.$2.$3-$4");
        }
        if (type == DocumentType.CNPJ && digits.length() == 14) {
            return digits.replaceFirst(
                    "(\\d{2})(\\d{3})(\\d{3})(\\d{4})(\\d{2})", "$1.$2.$3/$4-$5");
        }
        return number;
    }

    private String formatPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return "Telefone não informado";
        }
        String digits = phone.replaceAll("\\D", "");
        if (digits.length() == 11) {
            return digits.replaceFirst("(\\d{2})(\\d{5})(\\d{4})", "($1) $2-$3");
        }
        if (digits.length() == 10) {
            return digits.replaceFirst("(\\d{2})(\\d{4})(\\d{4})", "($1) $2-$3");
        }
        return phone;
    }

    private String formatCurrency(BigDecimal value) {
        return NumberFormat.getCurrencyInstance(PT_BR).format(value);
    }

    private String capitalize(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        return text.substring(0, 1).toUpperCase(PT_BR) + text.substring(1);
    }
}
