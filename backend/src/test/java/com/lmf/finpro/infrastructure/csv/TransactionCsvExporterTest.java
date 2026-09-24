package com.lmf.finpro.infrastructure.csv;

import static org.assertj.core.api.Assertions.assertThat;

import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.TransactionExportData;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;

class TransactionCsvExporterTest {

    private final TransactionCsvExporter exporter = new TransactionCsvExporter();

    @Test
    void producesUtf8BomFollowedByHeaderAndRows() {
        TransactionExportData data =
                new TransactionExportData(
                        YearMonth.of(2026, 9),
                        List.of(
                                new TransactionExportData.TransactionExportRow(
                                        LocalDate.of(2026, 9, 5),
                                        "Conta Corrente",
                                        "Aluguel",
                                        "",
                                        "Aluguel escritório",
                                        CategoryType.EXPENSE,
                                        new BigDecimal("1500.00")),
                                new TransactionExportData.TransactionExportRow(
                                        LocalDate.of(2026, 9, 10),
                                        "Carteira",
                                        "Sem categoria",
                                        "Cliente X",
                                        "Serviço prestado",
                                        CategoryType.INCOME,
                                        new BigDecimal("1000.00"))));

        byte[] csv = exporter.exportTransactionsToCsv(data);

        assertThat(csv[0]).isEqualTo((byte) 0xEF);
        assertThat(csv[1]).isEqualTo((byte) 0xBB);
        assertThat(csv[2]).isEqualTo((byte) 0xBF);

        String content = new String(csv, 3, csv.length - 3, StandardCharsets.UTF_8);
        assertThat(content)
                .isEqualTo(
                        "Data;Conta;Categoria;Cliente;Descrição;Tipo;Valor\r\n"
                                + "2026-09-05;Conta Corrente;Aluguel;;Aluguel escritório;Despesa;1500.00\r\n"
                                + "2026-09-10;Carteira;Sem categoria;Cliente X;Serviço prestado;Receita;1000.00\r\n");
    }

    @Test
    void quotesFieldsThatContainTheDelimiterOrQuotesButNotPlainCommas() {
        TransactionExportData data =
                new TransactionExportData(
                        YearMonth.of(2026, 9),
                        List.of(
                                new TransactionExportData.TransactionExportRow(
                                        LocalDate.of(2026, 9, 5),
                                        "Conta Corrente",
                                        "Sem categoria",
                                        "",
                                        "Compra em \"loja; ltda\", com desconto",
                                        CategoryType.EXPENSE,
                                        new BigDecimal("50.00"))));

        byte[] csv = exporter.exportTransactionsToCsv(data);
        String content = new String(csv, 3, csv.length - 3, StandardCharsets.UTF_8);

        assertThat(content).contains("\"Compra em \"\"loja; ltda\"\", com desconto\"");
    }

    @Test
    void producesOnlyTheHeaderWhenThereAreNoRows() {
        TransactionExportData data = new TransactionExportData(YearMonth.of(2026, 9), List.of());

        byte[] csv = exporter.exportTransactionsToCsv(data);
        String content = new String(csv, 3, csv.length - 3, StandardCharsets.UTF_8);

        assertThat(content).isEqualTo("Data;Conta;Categoria;Cliente;Descrição;Tipo;Valor\r\n");
    }
}
