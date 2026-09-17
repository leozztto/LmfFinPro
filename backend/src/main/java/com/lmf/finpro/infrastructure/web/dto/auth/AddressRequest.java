package com.lmf.finpro.infrastructure.web.dto.auth;

import com.lmf.finpro.domain.model.BrazilianState;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record AddressRequest(
    @NotBlank(message = "CEP é obrigatório")
    @Pattern(regexp = "\\d{8}", message = "CEP deve ter 8 dígitos")
    String zipCode,

    @NotBlank(message = "logradouro é obrigatório") String street,

    @NotBlank(message = "número é obrigatório") String number,

    String complement,

    @NotBlank(message = "bairro é obrigatório") String neighborhood,

    @NotBlank(message = "cidade é obrigatória") String city,

    @NotNull(message = "estado é obrigatório") BrazilianState state
) {
}
