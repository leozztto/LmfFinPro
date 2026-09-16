package com.lmf.finpro.infrastructure.web.dto.transaction;

import com.lmf.finpro.domain.model.CategoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionRequest(
    Long accountId,
    Long categoryId,
    Long clientId,
    @NotBlank(message = "descrição é obrigatória") String description,
    @NotNull(message = "valor é obrigatório") BigDecimal amount,
    @NotNull(message = "data é obrigatória") LocalDate transactionDate,
    @NotNull(message = "tipo é obrigatório") CategoryType type
) {
}
