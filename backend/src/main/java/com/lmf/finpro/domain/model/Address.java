package com.lmf.finpro.domain.model;

public record Address(
        String zipCode,
        String street,
        String number,
        String complement,
        String neighborhood,
        String city,
        BrazilianState state) {}
