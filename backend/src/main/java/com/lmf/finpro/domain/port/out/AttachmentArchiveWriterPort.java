package com.lmf.finpro.domain.port.out;

import com.lmf.finpro.domain.model.AttachmentArchiveData;
import java.io.OutputStream;

/** Escreve o pacote de comprovantes (ZIP com os arquivos + índice CSV) direto na saída. */
public interface AttachmentArchiveWriterPort {
    void write(AttachmentArchiveData data, OutputStream output);
}
