package com.lmf.finpro.infrastructure.web.dto.transaction;

import com.lmf.finpro.domain.model.TransactionStatus;
import jakarta.validation.constraints.NotNull;

public record TransactionStatusRequest(
        @NotNull(message = "situação é obrigatória") TransactionStatus status) {}
