package com.lmf.finpro.infrastructure.client.viacep;

public record ViaCepResponseDto(
        String cep,
        String logradouro,
        String complemento,
        String bairro,
        String localidade,
        String uf,
        Boolean erro) {}
