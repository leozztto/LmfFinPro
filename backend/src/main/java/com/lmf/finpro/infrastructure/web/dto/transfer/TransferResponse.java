package com.lmf.finpro.infrastructure.web.dto.transfer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record TransferResponse(
    Long id,
    Long fromAccountId,
    Long toAccountId,
    BigDecimal amount,
    LocalDate transferDate,
    String description,
    Long fromTransactionId,
    Long toTransactionId,
    LocalDateTime createdAt
) {
}
