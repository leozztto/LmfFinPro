package com.lmf.finpro.infrastructure.web.dto.profile;

import com.lmf.finpro.domain.model.BrazilianState;
import com.lmf.finpro.domain.model.DocumentType;
import com.lmf.finpro.domain.model.TaxRegime;
import java.time.LocalDateTime;

public record ProfileResponse(
        Long id,
        String name,
        String email,
        DocumentType documentType,
        String documentNumber,
        String phone,
        TaxRegime taxRegime,
        AddressResponse address,
        LocalDateTime createdAt) {

    public record AddressResponse(
            String zipCode,
            String street,
            String number,
            String complement,
            String neighborhood,
            String city,
            BrazilianState state) {}
}
