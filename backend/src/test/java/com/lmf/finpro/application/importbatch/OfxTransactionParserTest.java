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

class OfxTransactionParserTest {

    private static InputStream ofx(String content) {
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
    void parsesValidTransactionsWithSignedAmountsAndIgnoresUnknownTagsOutsideAndInsideBlocks() {
        String content =
                "OFXHEADER:100\n"
                        + "DATA:OFXSGML\n"
                        + "<OFX>\n"
                        + "<BANKTRANLIST>\n"
                        + "<STMTTRN>\n"
                        + "<TRNTYPE>DEBIT\n"
                        + "<DTPOSTED>20260901120000\n"
                        + "<TRNAMT>-1500.00\n"
                        + "<FITID>1001\n"
                        + "<MEMO>ALUGUEL ESCRITORIO\n"
                        + "</STMTTRN>\n"
                        + "<STMTTRN>\n"
                        + "<TRNTYPE>CREDIT\n"
                        + "<DTPOSTED>20260903\n"
                        + "<TRNAMT>4200.00\n"
                        + "<NAME>PAGAMENTO CLIENTE\n"
                        + "</STMTTRN>\n"
                        + "</BANKTRANLIST>\n"
                        + "</OFX>\n";

        List<ParsedTransactionRow> rows = OfxTransactionParser.parse(ofx(content));

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).date()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(rows.get(0).description()).isEqualTo("ALUGUEL ESCRITORIO");
        assertThat(rows.get(0).signedAmount()).isEqualByComparingTo("-1500.00");
        assertThat(rows.get(1).date()).isEqualTo(LocalDate.of(2026, 9, 3));
        assertThat(rows.get(1).description()).isEqualTo("PAGAMENTO CLIENTE");
        assertThat(rows.get(1).signedAmount()).isEqualByComparingTo("4200.00");
    }

    @Test
    void prefersMemoOverNameWhenBothArePresent() {
        String content =
                "<STMTTRN>\n"
                        + "<DTPOSTED>20260901\n"
                        + "<TRNAMT>-10.00\n"
                        + "<NAME>NOME GENERICO\n"
                        + "<MEMO>DESCRICAO DETALHADA\n"
                        + "</STMTTRN>\n";

        List<ParsedTransactionRow> rows = OfxTransactionParser.parse(ofx(content));

        assertThat(rows.get(0).description()).isEqualTo("DESCRICAO DETALHADA");
    }

    @Test
    void acceptsClosingTagsLikeWellFormedOfx2Xml() {
        String content =
                "<STMTTRN>\n"
                        + "<DTPOSTED>20260901</DTPOSTED>\n"
                        + "<TRNAMT>-10.00</TRNAMT>\n"
                        + "<MEMO>Compra</MEMO>\n"
                        + "</STMTTRN>\n";

        List<ParsedTransactionRow> rows = OfxTransactionParser.parse(ofx(content));

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).description()).isEqualTo("Compra");
    }

    @Test
    void rejectsFileWithNoStmttrnBlocks() {
        assertThatThrownBy(
                        () ->
                                OfxTransactionParser.parse(
                                        ofx("<OFX>\n<BANKTRANLIST>\n</BANKTRANLIST>\n</OFX>\n")))
                .isInstanceOf(ImportFileInvalidException.class);
    }

    @Test
    void rejectsEmptyFile() {
        assertThatThrownBy(() -> OfxTransactionParser.parse(ofx("")))
                .isInstanceOf(ImportFileInvalidException.class);
    }

    @Test
    void rejectsTransactionMissingDate() {
        String content = "<STMTTRN>\n<TRNAMT>-10.00\n<MEMO>Compra\n</STMTTRN>\n";

        assertThatThrownBy(() -> OfxTransactionParser.parse(ofx(content)))
                .isInstanceOf(ImportFileInvalidException.class);
    }

    @Test
    void rejectsTransactionMissingAmount() {
        String content = "<STMTTRN>\n<DTPOSTED>20260901\n<MEMO>Compra\n</STMTTRN>\n";

        assertThatThrownBy(() -> OfxTransactionParser.parse(ofx(content)))
                .isInstanceOf(ImportFileInvalidException.class);
    }

    @Test
    void rejectsTransactionMissingDescription() {
        String content = "<STMTTRN>\n<DTPOSTED>20260901\n<TRNAMT>-10.00\n</STMTTRN>\n";

        assertThatThrownBy(() -> OfxTransactionParser.parse(ofx(content)))
                .isInstanceOf(ImportFileInvalidException.class);
    }

    @Test
    void rejectsInvalidDate() {
        String content =
                "<STMTTRN>\n<DTPOSTED>31-13-2026\n<TRNAMT>-10.00\n<MEMO>Compra\n</STMTTRN>\n";

        assertThatThrownBy(() -> OfxTransactionParser.parse(ofx(content)))
                .isInstanceOf(ImportFileInvalidException.class);
    }

    @Test
    void rejectsInvalidAmount() {
        String content = "<STMTTRN>\n<DTPOSTED>20260901\n<TRNAMT>abc\n<MEMO>Compra\n</STMTTRN>\n";

        assertThatThrownBy(() -> OfxTransactionParser.parse(ofx(content)))
                .isInstanceOf(ImportFileInvalidException.class);
    }

    @Test
    void rejectsZeroAmount() {
        String content = "<STMTTRN>\n<DTPOSTED>20260901\n<TRNAMT>0\n<MEMO>Compra\n</STMTTRN>\n";

        assertThatThrownBy(() -> OfxTransactionParser.parse(ofx(content)))
                .isInstanceOf(ImportFileInvalidException.class);
    }

    @Test
    void wrapsIOExceptionFromTheUnderlyingStreamAsImportFileInvalidException() {
        assertThatThrownBy(() -> OfxTransactionParser.parse(brokenStream()))
                .isInstanceOf(ImportFileInvalidException.class)
                .hasMessage("Não foi possível ler o arquivo enviado.");
    }
}
