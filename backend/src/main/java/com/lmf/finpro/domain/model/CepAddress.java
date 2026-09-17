package com.lmf.finpro.domain.model;

public record CepAddress(
    String zipCode,
    String street,
    String complement,
    String neighborhood,
    String city,
    BrazilianState state
) {
}
