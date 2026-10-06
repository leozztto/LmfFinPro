package com.lmf.finpro.infrastructure.web.dto.household;

import jakarta.validation.constraints.NotNull;

public record TransferOwnershipRequest(
        @NotNull(message = "novo dono é obrigatório") Long newOwnerUserId) {}
