package com.lmf.finpro.infrastructure.web.validation;

import com.lmf.finpro.domain.model.DocumentType;
import com.lmf.finpro.domain.model.TaxRegime;

/**
 * Implementado pelo record de request cujo tipo de documento precisa ser coerente com o regime
 * tributário informado.
 */
public interface HasTaxRegimeDocument {
    TaxRegime taxRegime();

    DocumentType documentType();
}
