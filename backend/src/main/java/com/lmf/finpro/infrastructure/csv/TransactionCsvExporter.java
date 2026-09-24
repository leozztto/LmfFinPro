package com.lmf.finpro.infrastructure.csv;

import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.TransactionExportData;
import com.lmf.finpro.domain.port.out.TransactionExportPort;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

/**
 * Monta o CSV de exportação de transações na mão, mesma filosofia "sem lib pesada" do {@code
 * CsvTransactionParser} (aqui na direção inversa: escrita, não leitura). Inclui um BOM UTF-8 no
 * início do arquivo para o Excel reconhecer a acentuação corretamente ao abrir.
 */
@Component
public class TransactionCsvExporter implements TransactionExportPort {

    private static final byte[] UTF8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final char DELIMITER = ';';
    private static final String HEADER = "Data;Conta;Categoria;Cliente;Descrição;Tipo;Valor\r\n";

    @Override
    public byte[] exportTransactionsToCsv(TransactionExportData data) {
        StringBuilder csv = new StringBuilder(HEADER);
        for (TransactionExportData.TransactionExportRow row : data.rows()) {
            csv.append(row.date().format(DATE_FORMAT)).append(DELIMITER);
            csv.append(escape(row.accountName())).append(DELIMITER);
            csv.append(escape(row.categoryName())).append(DELIMITER);
            csv.append(escape(row.clientName())).append(DELIMITER);
            csv.append(escape(row.description())).append(DELIMITER);
            csv.append(row.type() == CategoryType.INCOME ? "Receita" : "Despesa").append(DELIMITER);
            csv.append(row.amount().toPlainString());
            csv.append("\r\n");
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            output.write(UTF8_BOM);
            output.write(csv.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao gerar o CSV de transações.", e);
        }
        return output.toByteArray();
    }

    private String escape(String value) {
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
