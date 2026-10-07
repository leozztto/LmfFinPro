package com.lmf.finpro.domain.port.out;

import com.lmf.finpro.domain.model.PersonalDataExport;
import java.io.OutputStream;

/** Escreve o pacote de portabilidade dos dados (ZIP) direto na saída. */
public interface PersonalDataArchiveWriterPort {
    void write(PersonalDataExport export, OutputStream output);
}
