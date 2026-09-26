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
        Long transferId,
        Long importBatchId,
        Long recurringTransactionId) {

    /** Transação sem vínculo com recorrência (manual, importada ou de transferência). */
    public Transaction(
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
            Long transferId,
            Long importBatchId) {
        this(
                id,
                accountId,
                categoryId,
                clientId,
                description,
                amount,
                transactionDate,
                type,
                origin,
                createdAt,
                transferId,
                importBatchId,
                null);
    }

    public static Transaction create(
            Long accountId,
            Long categoryId,
            Long clientId,
            String description,
            BigDecimal amount,
            LocalDate transactionDate,
            CategoryType type) {
        return new Transaction(
                null,
                accountId,
                categoryId,
                clientId,
                description,
                amount,
                transactionDate,
                type,
                TransactionOrigin.MANUAL,
                LocalDateTime.now(),
                null,
                null);
    }

    public static Transaction createForTransfer(
            Long accountId,
            String description,
            BigDecimal amount,
            LocalDate transactionDate,
            CategoryType type,
            Long transferId) {
        return new Transaction(
                null,
                accountId,
                null,
                null,
                description,
                amount,
                transactionDate,
                type,
                TransactionOrigin.MANUAL,
                LocalDateTime.now(),
                transferId,
                null);
    }

    public static Transaction createImported(
            Long accountId,
            Long categoryId,
            String description,
            BigDecimal amount,
            LocalDate transactionDate,
            CategoryType type,
            Long importBatchId) {
        return new Transaction(
                null,
                accountId,
                categoryId,
                null,
                description,
                amount,
                transactionDate,
                type,
                TransactionOrigin.IMPORTED,
                LocalDateTime.now(),
                null,
                importBatchId);
    }

    public Transaction withDetails(
            Long newCategoryId,
            Long newClientId,
            String newDescription,
            BigDecimal newAmount,
            LocalDate newTransactionDate,
            CategoryType newType) {
        return new Transaction(
                id,
                accountId,
                newCategoryId,
                newClientId,
                newDescription,
                newAmount,
                newTransactionDate,
                newType,
                origin,
                createdAt,
                transferId,
                importBatchId,
                recurringTransactionId);
    }

    /** Ocorrência de um {@link RecurringTransaction} lançada na data {@code occurrenceDate}. */
    public static Transaction createFromRecurrence(
            RecurringTransaction recurrence, LocalDate occurrenceDate) {
        return new Transaction(
                null,
                recurrence.accountId(),
                recurrence.categoryId(),
                recurrence.clientId(),
                recurrence.description(),
                recurrence.amount(),
                occurrenceDate,
                recurrence.type(),
                TransactionOrigin.RECURRING,
                LocalDateTime.now(),
                null,
                null,
                recurrence.id());
    }
}
