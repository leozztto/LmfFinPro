package com.lmf.finpro.application.auth;

import com.lmf.finpro.domain.model.Address;
import com.lmf.finpro.domain.model.BrazilianState;

public record AddressCommand(
        String zipCode,
        String street,
        String number,
        String complement,
        String neighborhood,
        String city,
        BrazilianState state) {

    /** CEP chega com ou sem máscara; no domínio fica só com dígitos. */
    public Address toDomain() {
        return new Address(
                zipCode == null ? null : zipCode.replaceAll("\\D", ""),
                street,
                number,
                complement,
                neighborhood,
                city,
                state);
    }
}
