package com.lmf.finpro.infrastructure.pdf;

import com.lmf.finpro.domain.model.Account;
import com.lmf.finpro.domain.model.AccountStatementData;
import com.lmf.finpro.domain.model.AccountType;
import com.lmf.finpro.domain.model.Address;
import com.lmf.finpro.domain.model.BudgetVsActualReportData;
import com.lmf.finpro.domain.model.CategoryExpenseReportData;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.Client;
import com.lmf.finpro.domain.model.ClientAnnualStatementData;
import com.lmf.finpro.domain.model.ClientReceiptData;
import com.lmf.finpro.domain.model.DocumentType;
import com.lmf.finpro.domain.model.IncomeStatementData;
import com.lmf.finpro.domain.model.ReportGranularity;
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
import java.time.Month;
import java.time.YearMonth;
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
    private static final Font EXCEEDED_FONT =
            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new Color(178, 34, 34));

    @Override
    public byte[] generateClientReceipt(ClientReceiptData data) {
        Document document = new Document(PageSize.A4, 36, 36, 40, 36);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(document, output);
            document.open();

            String monthLabel = monthLabel(data.referenceMonth());

            document.add(titleBlock("RECIBO DE PRESTAÇÃO DE SERVIÇOS", monthLabel));
            document.add(partyBlock("PRESTADOR DO SERVIÇO", issuerFields(data.issuer())));
            document.add(partyBlock("TOMADOR DO SERVIÇO", clientFields(data.client())));
            document.add(declarationBlock(data.client().name(), monthLabel, data.total()));
            document.add(transactionsBlock(data.transactions(), data.total()));
            document.add(placeDateSignatureBlock(data.issuer()));
            document.add(footer("recibo"));
        } catch (DocumentException e) {
            throw new IllegalStateException("Falha ao gerar o PDF do recibo.", e);
        } finally {
            document.close();
        }
        return output.toByteArray();
    }

    @Override
    public byte[] generateAccountStatement(AccountStatementData data) {
        Document document = new Document(PageSize.A4, 36, 36, 40, 36);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(document, output);
            document.open();

            String monthLabel = monthLabel(data.referenceMonth());

            document.add(titleBlock("EXTRATO DE CONTA", monthLabel));
            document.add(partyBlock("TITULAR", issuerFields(data.issuer())));
            document.add(accountBlock(data.account(), data.openingBalance()));
            document.add(statementTransactionsBlock(data));
            document.add(
                    statementTotalsBlock(
                            data.totalIncome(), data.totalExpense(), data.closingBalance()));
            document.add(footer("extrato"));
        } catch (DocumentException e) {
            throw new IllegalStateException("Falha ao gerar o PDF do extrato.", e);
        } finally {
            document.close();
        }
        return output.toByteArray();
    }

    @Override
    public byte[] generateClientAnnualStatement(ClientAnnualStatementData data) {
        Document document = new Document(PageSize.A4, 36, 36, 40, 36);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(document, output);
            document.open();

            String yearLabel = data.referenceYear().toString();

            document.add(titleBlock("DEMONSTRATIVO ANUAL DE RECEITA", yearLabel));
            document.add(partyBlock("PRESTADOR", issuerFields(data.issuer())));
            document.add(partyBlock("CLIENTE", clientFields(data.client())));
            document.add(annualIncomeBlock(data.monthlyIncomes(), data.totalYear()));
            document.add(footer("demonstrativo"));
        } catch (DocumentException e) {
            throw new IllegalStateException("Falha ao gerar o PDF do demonstrativo.", e);
        } finally {
            document.close();
        }
        return output.toByteArray();
    }

    @Override
    public byte[] generateCategoryExpenseReport(CategoryExpenseReportData data) {
        Document document = new Document(PageSize.A4, 36, 36, 40, 36);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(document, output);
            document.open();

            String monthLabel = monthLabel(data.referenceMonth());

            document.add(titleBlock("DESPESAS POR CATEGORIA", monthLabel));
            document.add(partyBlock("TITULAR", issuerFields(data.issuer())));
            document.add(categoryExpensesBlock(data.categoryExpenses(), data.totalExpense()));
            document.add(footer("relatório"));
        } catch (DocumentException e) {
            throw new IllegalStateException("Falha ao gerar o PDF do relatório.", e);
        } finally {
            document.close();
        }
        return output.toByteArray();
    }

    @Override
    public byte[] generateIncomeStatement(IncomeStatementData data) {
        Document document = new Document(PageSize.A4, 36, 36, 40, 36);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(document, output);
            document.open();

            String referenceLabel =
                    data.referenceYear() + " (" + granularityLabel(data.granularity()) + ")";

            document.add(titleBlock("RESULTADO DO PERÍODO (DRE)", referenceLabel));
            document.add(partyBlock("TITULAR", issuerFields(data.issuer())));
            document.add(
                    incomeStatementBlock(
                            data.periods(),
                            data.totalIncome(),
                            data.totalExpense(),
                            data.totalResult()));
            document.add(footer("relatório"));
        } catch (DocumentException e) {
            throw new IllegalStateException("Falha ao gerar o PDF do relatório.", e);
        } finally {
            document.close();
        }
        return output.toByteArray();
    }

    @Override
    public byte[] generateBudgetVsActualReport(BudgetVsActualReportData data) {
        Document document = new Document(PageSize.A4, 36, 36, 40, 36);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(document, output);
            document.open();

            String monthLabel = monthLabel(data.referenceMonth());

            document.add(titleBlock("ORÇAMENTO VS. REALIZADO", monthLabel));
            document.add(partyBlock("TITULAR", issuerFields(data.issuer())));
            document.add(
                    budgetVsActualBlock(data.comparisons(), data.totalLimit(), data.totalSpent()));
            document.add(footer("relatório"));
        } catch (DocumentException e) {
            throw new IllegalStateException("Falha ao gerar o PDF do relatório.", e);
        } finally {
            document.close();
        }
        return output.toByteArray();
    }

    private String monthLabel(YearMonth referenceMonth) {
        return monthLabelOnly(referenceMonth.getMonth()) + " de " + referenceMonth.getYear();
    }

    private Paragraph footer(String documentNoun) {
        Paragraph footer =
                new Paragraph(
                        "Documento gerado automaticamente pelo FinPro em "
                                + LocalDateTime.now().format(TIMESTAMP_FORMAT)
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
    private PdfPTable titleBlock(String titleText, String monthLabel) {
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
        table.addCell(valueCell(formatCurrency(openingBalance)));
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
                                (isIncome ? "+ " : "- ") + formatCurrency(transaction.amount()),
                                BODY_FONT));
                table.addCell(amountCell(formatCurrency(runningBalance), BODY_FONT));
            }
        }
        return table;
    }

    private PdfPTable statementTotalsBlock(
            BigDecimal totalIncome, BigDecimal totalExpense, BigDecimal closingBalance) {
        PdfPTable table = gridTable(new float[] {1f, 1f, 1f});

        table.addCell(headerCell("TOTAL DE RECEITAS"));
        table.addCell(headerCell("TOTAL DE DESPESAS"));
        table.addCell(headerCell("SALDO FINAL DO PERÍODO"));

        table.addCell(amountCell(formatCurrency(totalIncome), TOTAL_FONT));
        table.addCell(amountCell(formatCurrency(totalExpense), TOTAL_FONT));
        table.addCell(amountCell(formatCurrency(closingBalance), TOTAL_FONT));
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

    private String monthLabelOnly(Month month) {
        return capitalize(month.getDisplayName(TextStyle.FULL, PT_BR));
    }

    private String accountTypeLabel(AccountType type) {
        return switch (type) {
            case CHECKING -> "Conta corrente";
            case SAVINGS -> "Poupança";
            case WALLET -> "Carteira";
        };
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
