package com.lmf.finpro.infrastructure.web.dto.transfer;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransferRequest(
    @NotNull(message = "conta de origem é obrigatória") Long fromAccountId,
    @NotNull(message = "conta de destino é obrigatória") Long toAccountId,
    @NotNull(message = "valor é obrigatório")
    @DecimalMin(value = "0.01", message = "valor deve ser maior que zero") BigDecimal amount,
    @NotNull(message = "data é obrigatória") LocalDate transferDate,
    String description
) {
}
