package com.lmf.finpro.infrastructure.web.dto.tag;

import jakarta.validation.constraints.NotBlank;

/** {@code color} opcional, no formato {@code #RRGGBB}; o nome é normalizado no backend. */
public record TagRequest(@NotBlank(message = "nome é obrigatório") String name, String color) {}
