package com.lmf.finpro.application.importbatch;

import com.lmf.finpro.domain.exception.ImportFileInvalidException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Lê o formato de CSV suportado para importação de extrato: cabeçalho fixo
 * {@code date,description,amount}, data ISO (aaaa-mm-dd), valor com ponto decimal — positivo
 * é receita, negativo é despesa. O sinal é consumido aqui; o {@link ParsedRow} sempre carrega
 * um valor absoluto e cabe ao chamador decidir o {@code CategoryType} a partir do sinal original.
 */
public final class CsvTransactionParser {

    private static final String EXPECTED_HEADER = "date,description,amount";

    private CsvTransactionParser() {
    }

    public record ParsedRow(LocalDate date, String description, BigDecimal signedAmount) {
    }

    public static List<ParsedRow> parse(InputStream content) {
        List<ParsedRow> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(content, StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null || !normalizeHeader(headerLine).equals(EXPECTED_HEADER)) {
                throw new ImportFileInvalidException(
                    "Cabeçalho inválido. O arquivo deve começar com \"date,description,amount\"."
                );
            }

            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) {
                    continue;
                }
                rows.add(parseRow(line, lineNumber));
            }
        } catch (IOException e) {
            throw new ImportFileInvalidException("Não foi possível ler o arquivo enviado.");
        }

        if (rows.isEmpty()) {
            throw new ImportFileInvalidException("O arquivo não contém nenhuma transação para importar.");
        }
        return rows;
    }

    private static ParsedRow parseRow(String line, int lineNumber) {
        String[] columns = line.split(",", -1);
        if (columns.length != 3) {
            throw new ImportFileInvalidException("Linha " + lineNumber + ": esperado 3 colunas (date,description,amount).");
        }

        LocalDate date;
        try {
            date = LocalDate.parse(columns[0].trim());
        } catch (DateTimeParseException e) {
            throw new ImportFileInvalidException("Linha " + lineNumber + ": data inválida, use o formato aaaa-mm-dd.");
        }

        String description = columns[1].trim();
        if (description.isEmpty()) {
            throw new ImportFileInvalidException("Linha " + lineNumber + ": descrição é obrigatória.");
        }

        BigDecimal amount;
        try {
            amount = new BigDecimal(columns[2].trim());
        } catch (NumberFormatException e) {
            throw new ImportFileInvalidException("Linha " + lineNumber + ": valor inválido.");
        }
        if (amount.signum() == 0) {
            throw new ImportFileInvalidException("Linha " + lineNumber + ": valor não pode ser zero.");
        }

        return new ParsedRow(date, description, amount);
    }

    private static String normalizeHeader(String headerLine) {
        return headerLine.trim().toLowerCase().replaceAll("\\s*,\\s*", ",");
    }
}
