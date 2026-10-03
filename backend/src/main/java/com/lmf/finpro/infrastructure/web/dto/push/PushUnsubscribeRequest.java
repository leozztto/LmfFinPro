package com.lmf.finpro.infrastructure.web.dto.push;

import jakarta.validation.constraints.NotBlank;

public record PushUnsubscribeRequest(
        @NotBlank(message = "endpoint é obrigatório") String endpoint) {}
