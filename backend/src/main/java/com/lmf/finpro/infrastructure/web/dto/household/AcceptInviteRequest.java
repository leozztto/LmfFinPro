package com.lmf.finpro.infrastructure.web.dto.household;

import jakarta.validation.constraints.NotBlank;

public record AcceptInviteRequest(@NotBlank(message = "token é obrigatório") String token) {}
