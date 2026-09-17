package com.lmf.finpro.infrastructure.web.validation;

import com.lmf.finpro.domain.model.DocumentType;

/** Implementado pelo record de request cujo número de documento precisa ser validado conforme o tipo (CPF/CNPJ). */
public interface HasDocument {
    DocumentType documentType();
    String documentNumber();
}
