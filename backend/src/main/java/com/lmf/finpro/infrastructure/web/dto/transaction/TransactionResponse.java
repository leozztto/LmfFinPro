package com.lmf.finpro.infrastructure.web.dto.transaction;

import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.TransactionOrigin;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record TransactionResponse(
    Long id,
    Long accountId,
    Long categoryId,
    Long clientId,
    String description,
    BigDecimal amount,
    LocalDate transactionDate,
    CategoryType type,
    TransactionOrigin origin,
    LocalDateTime createdAt,
    Long transferId
) {
}
