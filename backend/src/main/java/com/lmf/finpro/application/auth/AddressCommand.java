package com.lmf.finpro.application.auth;

import com.lmf.finpro.domain.model.BrazilianState;

public record AddressCommand(
    String zipCode, String street, String number, String complement, String neighborhood, String city, BrazilianState state
) {
}
