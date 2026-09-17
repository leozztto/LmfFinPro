package com.lmf.finpro.application.auth;

import com.lmf.finpro.domain.model.DocumentType;

public record RegisterCommand(
    String name,
    String email,
    String rawPassword,
    DocumentType documentType,
    String documentNumber,
    String phone,
    String taxRegime,
    AddressCommand address
) {
}
