package com.lmf.finpro.application.auth;

import com.lmf.finpro.domain.model.DocumentType;
import com.lmf.finpro.domain.model.TaxRegime;

public record RegisterCommand(
    String name,
    String email,
    String rawPassword,
    DocumentType documentType,
    String documentNumber,
    String phone,
    TaxRegime taxRegime,
    AddressCommand address
) {
}
