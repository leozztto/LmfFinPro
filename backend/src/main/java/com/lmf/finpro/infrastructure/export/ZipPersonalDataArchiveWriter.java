package com.lmf.finpro.infrastructure.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.lmf.finpro.domain.model.PersonalDataExport;
import com.lmf.finpro.domain.port.out.FileStoragePort;
import com.lmf.finpro.domain.port.out.PersonalDataArchiveWriterPort;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * ZIP de portabilidade: {@code dados.json} (indentado, legível), os arquivos um por vez, lidos do
 * armazenamento sem montar o pacote na memória, e o {@code LEIA-ME.txt}. Um arquivo que sumiu do
 * disco não derruba a exportação: fica registrado no LEIA-ME.
 */
@Slf4j
@Component
public class ZipPersonalDataArchiveWriter implements PersonalDataArchiveWriterPort {

    private final ObjectMapper objectMapper;
    private final FileStoragePort fileStoragePort;

    public ZipPersonalDataArchiveWriter(
            ObjectMapper objectMapper, FileStoragePort fileStoragePort) {
        // Cópia: o mapper da aplicação não pode ganhar a indentação só por causa deste arquivo.
        this.objectMapper = objectMapper.copy().enable(SerializationFeature.INDENT_OUTPUT);
        this.fileStoragePort = fileStoragePort;
    }

    @Override
    public void write(PersonalDataExport export, OutputStream output) {
        List<String> missing = new ArrayList<>();
        try {
            ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8);
            zip.putNextEntry(new ZipEntry("dados.json"));
            zip.write(objectMapper.writeValueAsBytes(export.document()));
            zip.closeEntry();

            for (PersonalDataExport.File file : export.files()) {
                byte[] content;
                try {
                    content = fileStoragePort.load(file.storageKey());
                } catch (RuntimeException e) {
                    log.warn("Arquivo {} não encontrado ao exportar os dados", file.storageKey());
                    missing.add(file.path());
                    continue;
                }
                zip.putNextEntry(new ZipEntry(file.path()));
                zip.write(content);
                zip.closeEntry();
            }

            zip.putNextEntry(new ZipEntry("LEIA-ME.txt"));
            zip.write(readme(export.readme(), missing).getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            zip.finish();
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao gerar o pacote de exportação dos dados.", e);
        }
    }

    private static String readme(String base, List<String> missing) {
        if (missing.isEmpty()) {
            return base;
        }
        StringBuilder text = new StringBuilder(base);
        text.append("\nArquivos que não puderam ser incluídos\n");
        text.append("--------------------------------------\n");
        missing.forEach(path -> text.append("- ").append(path).append('\n'));
        return text.toString();
    }
}
