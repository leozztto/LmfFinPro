package com.lmf.finpro.infrastructure.web.dto.cep;

import com.lmf.finpro.domain.model.BrazilianState;

public record CepResponse(
    String zipCode,
    String street,
    String complement,
    String neighborhood,
    String city,
    BrazilianState state
) {
}
