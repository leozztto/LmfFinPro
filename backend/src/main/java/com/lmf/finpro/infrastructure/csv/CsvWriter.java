package com.lmf.finpro.infrastructure.csv;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Monta um arquivo CSV na mão a partir de linhas já formatadas como texto (cabeçalho incluso como a
 * primeira linha), mesma filosofia "sem lib pesada" do resto do backend. Usa {@code ;} como
 * delimitador (convenção do Excel em pt-BR) e inclui um BOM UTF-8 no início do arquivo para o Excel
 * reconhecer a acentuação corretamente ao abrir.
 */
public final class CsvWriter {

    private static final byte[] UTF8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
    private static final char DELIMITER = ';';

    private CsvWriter() {}

    public static byte[] write(List<String[]> rows) {
        StringBuilder csv = new StringBuilder();
        for (String[] row : rows) {
            for (int i = 0; i < row.length; i++) {
                if (i > 0) {
                    csv.append(DELIMITER);
                }
                csv.append(escape(row[i]));
            }
            csv.append("\r\n");
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            output.write(UTF8_BOM);
            output.write(csv.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao gerar o CSV.", e);
        }
        return output.toByteArray();
    }

    private static String escape(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        boolean needsQuoting =
                value.indexOf(DELIMITER) >= 0
                        || value.contains("\"")
                        || value.contains("\n")
                        || value.contains("\r");
        String escaped = value.replace("\"", "\"\"");
        return needsQuoting ? "\"" + escaped + "\"" : escaped;
    }
}
