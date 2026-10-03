package com.lmf.finpro.infrastructure.pdf;

import com.lmf.finpro.domain.model.Address;
import com.lmf.finpro.domain.model.Client;
import com.lmf.finpro.domain.model.Currency;
import com.lmf.finpro.domain.model.DocumentType;
import com.lmf.finpro.domain.model.TransactionStatus;
import com.lmf.finpro.domain.model.User;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

/**
 * Base dos geradores de PDF por tipo de relatório: o layout "grade contínua" (tabelas de largura
 * total empilhadas, cabeçalhos em azul-claro), as células, as fontes e as formatações em pt-BR
 * ficam aqui, e cada subclasse só monta os blocos do seu relatório.
 */
abstract class PdfReportSupport {

    static final Locale PT_BR = Locale.of("pt", "BR");
    static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    static final Color HEADER_BACKGROUND = new Color(214, 230, 247);
    static final Color BORDER_COLOR = new Color(120, 150, 185);

    static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15);
    static final Font HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);
    static final Font LABEL_FONT = FontFactory.getFont(FontFactory.HELVETICA, 8);
    static final Font BODY_FONT = FontFactory.getFont(FontFactory.HELVETICA, 10);
    static final Font BODY_BOLD_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
    static final Font TOTAL_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
    static final Font FOOTER_FONT = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 7);
    static final Font EXCEEDED_FONT =
            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new Color(178, 34, 34));

    /** Corpo de um documento: adiciona os blocos já com o {@link Document} aberto. */
    @FunctionalInterface
    interface DocumentBody {
        void write(Document document) throws DocumentException;
    }

    /** Relógio de São Paulo: a data de emissão e o rodapé não podem sair no fuso do servidor. */
    private final Clock clock;

    PdfReportSupport(Clock clock) {
        this.clock = clock;
    }

    /** Abre o documento, executa o corpo e devolve os bytes; falha vira {@code IllegalState}. */
    byte[] render(Rectangle pageSize, String failureMessage, DocumentBody body) {
        Document document = new Document(pageSize, 36, 36, 40, 36);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(document, output);
            document.open();
            body.write(document);
        } catch (DocumentException e) {
            throw new IllegalStateException(failureMessage, e);
        } finally {
            document.close();
        }
        return output.toByteArray();
    }

    String statusLabel(TransactionStatus status) {
        return status == TransactionStatus.PAID ? "Paga" : "Pendente";
    }

    PdfPCell rightLabelCell(String text) {
        PdfPCell cell = labelCell(text);
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return cell;
    }

    PdfPTable appliedFiltersBlock(List<String> appliedFilters) {
        PdfPTable table = gridTable(new float[] {1f});
        table.addCell(headerCell("FILTROS APLICADOS"));
        table.addCell(
                valueCell(
                        appliedFilters.isEmpty()
                                ? "Nenhum filtro além do período."
                                : String.join("  ·  ", appliedFilters)));
        return table;
    }

    String monthLabel(YearMonth referenceMonth) {
        return monthLabelOnly(referenceMonth.getMonth()) + " de " + referenceMonth.getYear();
    }

    Paragraph footer(String documentNoun) {
        Paragraph footer =
                new Paragraph(
                        "Documento gerado automaticamente pelo FinPro em "
                                + LocalDateTime.now(clock).format(TIMESTAMP_FORMAT)
                                + ". Este "
                                + documentNoun
                                + " não possui validade fiscal.",
                        FOOTER_FONT);
        footer.setSpacingBefore(6);
        return footer;
    }

    /**
     * Título à esquerda; à direita, os cabeçalhos "Referência" e "Emissão" com os valores abaixo.
     */
    PdfPTable titleBlock(String titleText, String monthLabel) {
        PdfPTable table = gridTable(new float[] {4f, 1.3f, 1.3f});

        PdfPCell title = new PdfPCell(new Paragraph(titleText, TITLE_FONT));
        title.setRowspan(2);
        title.setVerticalAlignment(Element.ALIGN_MIDDLE);
        title.setPadding(10);
        title.setBorderColor(BORDER_COLOR);
        table.addCell(title);

        table.addCell(headerCell("REFERÊNCIA"));
        table.addCell(headerCell("EMISSÃO"));
        table.addCell(centeredValueCell(monthLabel));
        table.addCell(centeredValueCell(LocalDateTime.now(clock).format(DATE_FORMAT)));
        return table;
    }

    /**
     * Faixa de cabeçalho com o nome da parte e, abaixo, pares "rótulo | valor" em duas colunas. Um
     * campo com terceiro elemento (ex.: Endereço) é tratado como largo e ocupa a linha inteira.
     */
    PdfPTable partyBlock(String title, String[][] fields) {
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

    String monthLabelOnly(Month month) {
        return capitalize(month.getDisplayName(TextStyle.FULL, PT_BR));
    }

    String[][] issuerFields(User issuer) {
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

    String[][] clientFields(Client client) {
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

    PdfPTable gridTable(float[] widths) {
        PdfPTable table = new PdfPTable(widths);
        table.setWidthPercentage(100);
        table.setSpacingBefore(0);
        table.setSpacingAfter(0);
        return table;
    }

    PdfPCell headerCell(String text) {
        PdfPCell cell = new PdfPCell(new Paragraph(text, HEADER_FONT));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBackgroundColor(HEADER_BACKGROUND);
        cell.setBorderColor(BORDER_COLOR);
        cell.setPadding(4);
        return cell;
    }

    PdfPCell labelCell(String text) {
        PdfPCell cell = new PdfPCell(new Paragraph(text.toUpperCase(PT_BR), LABEL_FONT));
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBackgroundColor(HEADER_BACKGROUND);
        cell.setBorderColor(BORDER_COLOR);
        cell.setPadding(4);
        return cell;
    }

    PdfPCell valueCell(String text) {
        PdfPCell cell = new PdfPCell(new Paragraph(text == null ? "" : text, BODY_FONT));
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBorderColor(BORDER_COLOR);
        cell.setPadding(5);
        return cell;
    }

    /** Descrição e, logo abaixo e em fonte menor, as tags — sem ocupar mais uma coluna. */
    PdfPCell descriptionWithTagsCell(String description, String tags) {
        PdfPCell cell = valueCell(description);
        if (tags != null && !tags.isBlank()) {
            Paragraph tagsLine = new Paragraph(tags, LABEL_FONT);
            tagsLine.setSpacingBefore(1);
            cell.addElement(new Paragraph(description == null ? "" : description, BODY_FONT));
            cell.addElement(tagsLine);
        }
        return cell;
    }

    PdfPCell centeredValueCell(String text) {
        PdfPCell cell = valueCell(text);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        return cell;
    }

    PdfPCell amountCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Paragraph(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBorderColor(BORDER_COLOR);
        cell.setPadding(6);
        return cell;
    }

    String documentLabel(DocumentType type) {
        return type == null ? "Documento" : type.name();
    }

    String formatAddress(Address address) {
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

    String formatDocument(DocumentType type, String number) {
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

    String formatPhone(String phone) {
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

    String formatCurrency(BigDecimal value) {
        return NumberFormat.getCurrencyInstance(PT_BR).format(value);
    }

    /** Na moeda informada, no formato brasileiro (ex.: "US$ 1.234,56"). */
    String formatCurrency(BigDecimal value, Currency currency) {
        NumberFormat format = NumberFormat.getCurrencyInstance(PT_BR);
        format.setCurrency(java.util.Currency.getInstance(currency.name()));
        return format.format(value);
    }

    String capitalize(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        return text.substring(0, 1).toUpperCase(PT_BR) + text.substring(1);
    }
}
