package com.lmf.finpro.application.importbatch;

import com.lmf.finpro.domain.exception.ImportFileInvalidException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Lê o formato de CSV suportado para importação de extrato: cabeçalho {@code
 * date,description,amount} (sem hora) ou, opcionalmente, {@code date,time,description,amount} (com
 * hora — {@code HH:mm} ou {@code HH:mm:ss}, célula vazia vira {@code null}). Data ISO (aaaa-mm-dd),
 * valor com ponto decimal — positivo é receita, negativo é despesa. O sinal é preservado em {@link
 * ParsedTransactionRow#signedAmount()}; cabe ao chamador decidir o {@code CategoryType} a partir
 * dele.
 */
public final class CsvTransactionParser {

    private static final String HEADER_WITHOUT_TIME = "date,description,amount";
    private static final String HEADER_WITH_TIME = "date,time,description,amount";
    private static final DateTimeFormatter TIME_WITH_SECONDS =
            DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter TIME_WITHOUT_SECONDS =
            DateTimeFormatter.ofPattern("HH:mm");

    private CsvTransactionParser() {}

    public static List<ParsedTransactionRow> parse(InputStream content) {
        List<ParsedTransactionRow> rows = new ArrayList<>();
        try (BufferedReader reader =
                new BufferedReader(new InputStreamReader(content, StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            boolean hasTimeColumn = requireValidHeader(headerLine);

            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) {
                    continue;
                }
                rows.add(parseRow(line, lineNumber, hasTimeColumn));
            }
        } catch (IOException e) {
            throw new ImportFileInvalidException("Não foi possível ler o arquivo enviado.");
        }

        if (rows.isEmpty()) {
            throw new ImportFileInvalidException(
                    "O arquivo não contém nenhuma transação para importar.");
        }
        return rows;
    }

    /**
     * @return se o cabeçalho tem a coluna opcional de hora
     */
    private static boolean requireValidHeader(String headerLine) {
        String normalized = headerLine == null ? null : normalizeHeader(headerLine);
        if (HEADER_WITHOUT_TIME.equals(normalized)) {
            return false;
        }
        if (HEADER_WITH_TIME.equals(normalized)) {
            return true;
        }
        throw new ImportFileInvalidException(
                "Cabeçalho inválido. O arquivo deve começar com \"date,description,amount\" ou, para"
                        + " incluir a hora, \"date,time,description,amount\".");
    }

    private static ParsedTransactionRow parseRow(
            String line, int lineNumber, boolean hasTimeColumn) {
        String[] columns = line.split(",", -1);
        int expectedColumns = hasTimeColumn ? 4 : 3;
        if (columns.length != expectedColumns) {
            throw new ImportFileInvalidException(
                    "Linha "
                            + lineNumber
                            + ": esperado "
                            + expectedColumns
                            + " colunas ("
                            + (hasTimeColumn
                                    ? "date,time,description,amount"
                                    : "date,description,amount")
                            + ").");
        }

        LocalDate date;
        try {
            date = LocalDate.parse(columns[0].trim());
        } catch (DateTimeParseException e) {
            throw new ImportFileInvalidException(
                    "Linha " + lineNumber + ": data inválida, use o formato aaaa-mm-dd.");
        }

        int column = 1;
        LocalTime time = null;
        if (hasTimeColumn) {
            String rawTime = columns[column].trim();
            if (!rawTime.isEmpty()) {
                time = parseTime(rawTime, lineNumber);
            }
            column++;
        }

        String description = columns[column].trim();
        if (description.isEmpty()) {
            throw new ImportFileInvalidException(
                    "Linha " + lineNumber + ": descrição é obrigatória.");
        }
        column++;

        BigDecimal amount;
        try {
            amount = new BigDecimal(columns[column].trim());
        } catch (NumberFormatException e) {
            throw new ImportFileInvalidException("Linha " + lineNumber + ": valor inválido.");
        }
        if (amount.signum() == 0) {
            throw new ImportFileInvalidException(
                    "Linha " + lineNumber + ": valor não pode ser zero.");
        }

        return new ParsedTransactionRow(date, time, description, amount);
    }

    private static LocalTime parseTime(String rawTime, int lineNumber) {
        try {
            return LocalTime.parse(rawTime, TIME_WITH_SECONDS);
        } catch (DateTimeParseException e) {
            try {
                return LocalTime.parse(rawTime, TIME_WITHOUT_SECONDS);
            } catch (DateTimeParseException e2) {
                throw new ImportFileInvalidException(
                        "Linha "
                                + lineNumber
                                + ": hora inválida, use o formato HH:mm ou HH:mm:ss.");
            }
        }
    }

    private static String normalizeHeader(String headerLine) {
        return headerLine.trim().toLowerCase().replaceAll("\\s*,\\s*", ",");
    }
}
