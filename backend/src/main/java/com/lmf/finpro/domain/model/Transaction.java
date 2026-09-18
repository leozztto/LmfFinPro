package com.lmf.finpro.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record Transaction(
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

    public static Transaction create(
        Long accountId,
        Long categoryId,
        Long clientId,
        String description,
        BigDecimal amount,
        LocalDate transactionDate,
        CategoryType type
    ) {
        return new Transaction(
            null, accountId, categoryId, clientId, description, amount,
            transactionDate, type, TransactionOrigin.MANUAL, LocalDateTime.now(), null
        );
    }

    public static Transaction createForTransfer(
        Long accountId,
        String description,
        BigDecimal amount,
        LocalDate transactionDate,
        CategoryType type,
        Long transferId
    ) {
        return new Transaction(
            null, accountId, null, null, description, amount,
            transactionDate, type, TransactionOrigin.MANUAL, LocalDateTime.now(), transferId
        );
    }

    public Transaction withDetails(
        Long newCategoryId,
        Long newClientId,
        String newDescription,
        BigDecimal newAmount,
        LocalDate newTransactionDate,
        CategoryType newType
    ) {
        return new Transaction(
            id, accountId, newCategoryId, newClientId, newDescription, newAmount,
            newTransactionDate, newType, origin, createdAt, transferId
        );
    }
}
