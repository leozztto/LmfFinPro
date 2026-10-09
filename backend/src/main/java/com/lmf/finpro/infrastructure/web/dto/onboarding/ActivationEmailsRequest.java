package com.lmf.finpro.infrastructure.web.dto.onboarding;

import jakarta.validation.constraints.NotNull;

public record ActivationEmailsRequest(
        @NotNull(message = "enabled é obrigatório") Boolean enabled) {}
