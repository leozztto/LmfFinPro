package com.lmf.finpro.application.importbatch;

import com.lmf.finpro.domain.exception.ImportFileInvalidException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Lê o formato OFX (Open Financial Exchange) suportado para importação de extrato: um elemento por
 * linha, com ou sem fechamento de tag — cobre tanto OFX 1.x estilo SGML (ex.: {@code
 * <DTPOSTED>20260901120000}, sem {@code </DTPOSTED>}), o padrão mais comum em extratos exportados
 * por bancos brasileiros, quanto OFX 2.x XML bem formado (ex.: {@code <MEMO>Aluguel</MEMO>}), desde
 * que cada elemento continue em sua própria linha. Cada bloco {@code <STMTTRN>...</STMTTRN>} vira
 * uma {@link ParsedTransactionRow}; o sinal de {@code TRNAMT} é preservado, igual ao parser de CSV.
 */
public final class OfxTransactionParser {

    private static final String STMTTRN_OPEN = "<STMTTRN>";
    private static final String STMTTRN_CLOSE = "</STMTTRN>";
    private static final String DTPOSTED = "<DTPOSTED>";
    private static final String TRNAMT = "<TRNAMT>";
    private static final String MEMO = "<MEMO>";
    private static final String NAME = "<NAME>";
    private static final DateTimeFormatter OFX_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private OfxTransactionParser() {}

    public static List<ParsedTransactionRow> parse(InputStream content) {
        List<String> lines = readLines(content);

        List<ParsedTransactionRow> rows = new ArrayList<>();
        boolean inTransaction = false;
        int transactionStartLine = -1;
        String date = null;
        String amount = null;
        String memo = null;
        String name = null;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            String upper = line.toUpperCase(Locale.ROOT);

            if (upper.startsWith(STMTTRN_OPEN)) {
                inTransaction = true;
                transactionStartLine = i + 1;
                date = null;
                amount = null;
                memo = null;
                name = null;
                continue;
            }
            if (upper.startsWith(STMTTRN_CLOSE)) {
                if (inTransaction) {
                    rows.add(buildRow(transactionStartLine, date, amount, memo, name));
                }
                inTransaction = false;
                continue;
            }
            if (!inTransaction) {
                continue;
            }

            if (upper.startsWith(DTPOSTED)) {
                date = extractValue(line, DTPOSTED.length());
            } else if (upper.startsWith(TRNAMT)) {
                amount = extractValue(line, TRNAMT.length());
            } else if (upper.startsWith(MEMO)) {
                memo = extractValue(line, MEMO.length());
            } else if (upper.startsWith(NAME)) {
                name = extractValue(line, NAME.length());
            }
        }

        if (rows.isEmpty()) {
            throw new ImportFileInvalidException(
                    "O arquivo OFX não contém nenhuma transação (bloco <STMTTRN>) para importar.");
        }
        return rows;
    }

    private static List<String> readLines(InputStream content) {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader =
                new BufferedReader(new InputStreamReader(content, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            throw new ImportFileInvalidException("Não foi possível ler o arquivo enviado.");
        }
        return lines;
    }

    private static ParsedTransactionRow buildRow(
            int lineNumber, String date, String amount, String memo, String name) {
        if (date == null || date.isBlank()) {
            throw new ImportFileInvalidException(
                    "Transação sem data (DTPOSTED) na linha " + lineNumber + ".");
        }
        if (amount == null || amount.isBlank()) {
            throw new ImportFileInvalidException(
                    "Transação sem valor (TRNAMT) na linha " + lineNumber + ".");
        }
        String description = (memo != null && !memo.isBlank()) ? memo : name;
        if (description == null || description.isBlank()) {
            throw new ImportFileInvalidException(
                    "Transação sem descrição (MEMO/NAME) na linha " + lineNumber + ".");
        }

        LocalDate parsedDate;
        try {
            parsedDate = LocalDate.parse(date.substring(0, Math.min(8, date.length())), OFX_DATE);
        } catch (DateTimeParseException e) {
            throw new ImportFileInvalidException(
                    "Linha " + lineNumber + ": data inválida em DTPOSTED (\"" + date + "\").");
        }

        BigDecimal signedAmount;
        try {
            signedAmount = new BigDecimal(amount.trim());
        } catch (NumberFormatException e) {
            throw new ImportFileInvalidException(
                    "Linha " + lineNumber + ": valor inválido em TRNAMT (\"" + amount + "\").");
        }
        if (signedAmount.signum() == 0) {
            throw new ImportFileInvalidException(
                    "Linha " + lineNumber + ": valor não pode ser zero.");
        }

        return new ParsedTransactionRow(parsedDate, description.trim(), signedAmount);
    }

    private static String extractValue(String line, int openTagLength) {
        String value = line.substring(openTagLength);
        int closingTagIndex = value.indexOf('<');
        if (closingTagIndex >= 0) {
            value = value.substring(0, closingTagIndex);
        }
        return value.trim();
    }
}
