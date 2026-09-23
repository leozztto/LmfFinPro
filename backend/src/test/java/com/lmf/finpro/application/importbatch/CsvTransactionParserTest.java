package com.lmf.finpro.application.importbatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lmf.finpro.domain.exception.ImportFileInvalidException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class CsvTransactionParserTest {

    private static InputStream csv(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    private static InputStream brokenStream() {
        return new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("falha simulada de leitura");
            }
        };
    }

    @Test
    void parsesValidRowsWithSignedAmounts() {
        String content =
                "date,description,amount\n"
                        + "2026-09-01,Salario,3000.00\n"
                        + "2026-09-02,Aluguel,-1500.50\n";

        List<ParsedTransactionRow> rows = CsvTransactionParser.parse(csv(content));

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).date()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(rows.get(0).description()).isEqualTo("Salario");
        assertThat(rows.get(0).signedAmount()).isEqualByComparingTo("3000.00");
        assertThat(rows.get(1).signedAmount()).isEqualByComparingTo("-1500.50");
    }

    @Test
    void skipsBlankLinesBetweenRows() {
        String content =
                "date,description,amount\n"
                        + "2026-09-01,Salario,3000.00\n"
                        + "\n"
                        + "2026-09-02,Aluguel,-100\n";

        List<ParsedTransactionRow> rows = CsvTransactionParser.parse(csv(content));

        assertThat(rows).hasSize(2);
    }

    @Test
    void rejectsMissingOrWrongHeader() {
        assertThatThrownBy(
                        () ->
                                CsvTransactionParser.parse(
                                        csv("data,descricao,valor\n2026-09-01,x,1\n")))
                .isInstanceOf(ImportFileInvalidException.class);
    }

    @Test
    void rejectsEmptyFile() {
        assertThatThrownBy(() -> CsvTransactionParser.parse(csv("")))
                .isInstanceOf(ImportFileInvalidException.class);
    }

    @Test
    void rejectsFileWithOnlyHeaderAndNoDataRows() {
        assertThatThrownBy(() -> CsvTransactionParser.parse(csv("date,description,amount\n")))
                .isInstanceOf(ImportFileInvalidException.class);
    }

    @Test
    void rejectsRowWithWrongColumnCount() {
        assertThatThrownBy(
                        () ->
                                CsvTransactionParser.parse(
                                        csv("date,description,amount\n2026-09-01,Salario\n")))
                .isInstanceOf(ImportFileInvalidException.class);
    }

    @Test
    void rejectsRowWithInvalidDate() {
        assertThatThrownBy(
                        () ->
                                CsvTransactionParser.parse(
                                        csv("date,description,amount\n01-09-2026,Salario,100\n")))
                .isInstanceOf(ImportFileInvalidException.class);
    }

    @Test
    void rejectsRowWithEmptyDescription() {
        assertThatThrownBy(
                        () ->
                                CsvTransactionParser.parse(
                                        csv("date,description,amount\n2026-09-01,,100\n")))
                .isInstanceOf(ImportFileInvalidException.class);
    }

    @Test
    void rejectsRowWithInvalidAmount() {
        assertThatThrownBy(
                        () ->
                                CsvTransactionParser.parse(
                                        csv("date,description,amount\n2026-09-01,Salario,abc\n")))
                .isInstanceOf(ImportFileInvalidException.class);
    }

    @Test
    void rejectsRowWithZeroAmount() {
        assertThatThrownBy(
                        () ->
                                CsvTransactionParser.parse(
                                        csv("date,description,amount\n2026-09-01,Salario,0\n")))
                .isInstanceOf(ImportFileInvalidException.class);
    }

    @Test
    void wrapsIOExceptionFromTheUnderlyingStreamAsImportFileInvalidException() {
        assertThatThrownBy(() -> CsvTransactionParser.parse(brokenStream()))
                .isInstanceOf(ImportFileInvalidException.class)
                .hasMessage("Não foi possível ler o arquivo enviado.");
    }

    @Test
    void acceptsHeaderWithExtraWhitespaceAndDifferentCase() {
        String content = "Date, Description , Amount\n2026-09-01,Salario,100\n";

        List<ParsedTransactionRow> rows = CsvTransactionParser.parse(csv(content));

        assertThat(rows).hasSize(1);
    }
}
