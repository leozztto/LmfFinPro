package com.lmf.finpro.infrastructure.web.dto.transfer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * @param amount valor que saiu da conta de origem, na moeda dela
 * @param receivedAmount valor que entrou na conta de destino, na moeda dela (igual a {@code amount}
 *     entre contas da mesma moeda)
 * @param fromAccountName nome da conta de origem, mesmo que seja de outro espaço (pessoal x grupo);
 *     só o nome, nenhum outro dado dela
 * @param toAccountName nome da conta de destino, nas mesmas condições
 * @param createdByUserId quem criou a transferência; nulo quando não se sabe. Numa conta
 *     compartilhada só essa pessoa pode excluí-la
 * @param createdByName nome de quem criou
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
        BigDecimal receivedAmount,
        String fromAccountName,
        String toAccountName,
        Long createdByUserId,
        String createdByName) {}
