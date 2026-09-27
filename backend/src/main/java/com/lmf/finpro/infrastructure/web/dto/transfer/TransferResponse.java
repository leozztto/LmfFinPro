package com.lmf.finpro.infrastructure.web.dto.transfer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * @param amount valor que saiu da conta de origem, na moeda dela
 * @param receivedAmount valor que entrou na conta de destino, na moeda dela (igual a {@code amount}
 *     entre contas da mesma moeda)
 */
public record TransferResponse(
        Long id,
        Long fromAccountId,
        Long toAccountId,
        BigDecimal amount,
        LocalDate transferDate,
        String description,
        Long fromTransactionId,
        Long toTransactionId,
        LocalDateTime createdAt,
        BigDecimal receivedAmount) {}
