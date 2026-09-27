package com.lmf.finpro.infrastructure.csv;

import com.lmf.finpro.domain.model.AttachmentArchiveData;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.TransactionStatus;
import com.lmf.finpro.domain.port.out.AttachmentArchiveWriterPort;
import com.lmf.finpro.domain.port.out.FileStoragePort;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * ZIP com os comprovantes (um por vez, sem carregar o pacote inteiro na memória) e, na raiz, o
 * índice {@code comprovantes-AAAA.csv} — mesmo formato dos outros CSVs do app (BOM UTF-8, ";"),
 * para abrir direto no Excel.
 */
@Component
@RequiredArgsConstructor
public class ZipAttachmentArchiveWriter implements AttachmentArchiveWriterPort {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final FileStoragePort fileStoragePort;

    @Override
    public void write(AttachmentArchiveData data, OutputStream output) {
        try {
            ZipOutputStream zip = new ZipOutputStream(output);
            for (AttachmentArchiveData.Entry entry : data.entries()) {
                zip.putNextEntry(new ZipEntry(entry.path()));
                zip.write(fileStoragePort.load(entry.storageKey()));
                zip.closeEntry();
            }
            zip.putNextEntry(new ZipEntry("comprovantes-" + data.year() + ".csv"));
            zip.write(CsvWriter.write(indexRows(data)));
            zip.closeEntry();
            zip.finish();
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao gerar o pacote de comprovantes.", e);
        }
    }

    private List<String[]> indexRows(AttachmentArchiveData data) {
        List<String[]> rows = new ArrayList<>();
        rows.add(
                new String[] {
                    "Arquivo",
                    "Data",
                    "Descrição",
                    "Tipo",
                    "Valor",
                    "Situação",
                    "Conta",
                    "Categoria",
                    "Cliente",
                    "Documento",
                    "Nome original",
                    "Tags"
                });
        for (AttachmentArchiveData.Entry entry : data.entries()) {
            rows.add(
                    new String[] {
                        entry.path(),
                        entry.date().format(DATE_FORMAT),
                        entry.description(),
                        entry.type() == CategoryType.INCOME ? "Receita" : "Despesa",
                        entry.amount().toPlainString(),
                        entry.status() == TransactionStatus.PAID ? "Paga" : "Pendente",
                        entry.accountName(),
                        entry.categoryName(),
                        entry.clientName(),
                        entry.documentType().label(),
                        entry.originalFileName(),
                        entry.tags()
                    });
        }
        return rows;
    }
}
