package com.lmf.finpro.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * @param amount valor na moeda da conta — é o que mexe no saldo dela
 * @param originalCurrency moeda em que a operação foi feita, quando diferente da moeda da conta
 *     (ex.: compra em dólar no cartão em reais); {@code null} quando é a mesma
 * @param originalAmount valor na {@code originalCurrency}; {@code null} junto com ela
 * @param baseAmount valor em reais, usado em todo total que junta contas (dashboard, relatórios,
 *     orçamentos). Na conta em reais é o próprio {@code amount}; nas demais, a conversão pela
 *     cotação do dia da transação, gravada ao salvar
 */
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
        Long recurringTransactionId,
        TransactionStatus status,
        Currency originalCurrency,
        BigDecimal originalAmount,
        BigDecimal baseAmount) {

    /** Transação sem moeda estrangeira: o valor em reais é o próprio {@code amount}. */
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
            Long importBatchId,
            Long recurringTransactionId,
            TransactionStatus status) {
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
                recurringTransactionId,
                status,
                null,
                null,
                amount);
    }

    /** Transação paga e sem vínculo com recorrência (manual, importada ou de transferência). */
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
                null,
                TransactionStatus.PAID);
    }

    /** Lançamento manual já pago. */
    public static Transaction create(
            Long accountId,
            Long categoryId,
            Long clientId,
            String description,
            BigDecimal amount,
            LocalDate transactionDate,
            CategoryType type) {
        return create(
                accountId,
                categoryId,
                clientId,
                description,
                amount,
                transactionDate,
                type,
                TransactionStatus.PAID);
    }

    public static Transaction create(
            Long accountId,
            Long categoryId,
            Long clientId,
            String description,
            BigDecimal amount,
            LocalDate transactionDate,
            CategoryType type,
            TransactionStatus status) {
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
                null,
                null,
                status);
    }

    /** Transferência movimenta dinheiro na hora: as duas pontas são sempre pagas. */
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

    /** Linha de extrato bancário: se está no extrato, já aconteceu — sempre paga. */
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

    /**
     * Novos dados. O valor em reais volta a ser o próprio {@code newAmount}: quem salva a transação
     * de uma conta em outra moeda converte de novo ({@link #withBaseAmount}).
     */
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
                recurringTransactionId,
                status,
                originalCurrency,
                originalAmount,
                newAmount);
    }

    public Transaction withStatus(TransactionStatus newStatus) {
        return new Transaction(
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
                recurringTransactionId,
                newStatus,
                originalCurrency,
                originalAmount,
                baseAmount);
    }

    /** Moeda e valor da operação feita numa moeda diferente da conta; nulos quando não. */
    public Transaction withOriginal(Currency newOriginalCurrency, BigDecimal newOriginalAmount) {
        return new Transaction(
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
                recurringTransactionId,
                status,
                newOriginalCurrency,
                newOriginalAmount,
                baseAmount);
    }

    public Transaction withBaseAmount(BigDecimal newBaseAmount) {
        return new Transaction(
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
                recurringTransactionId,
                status,
                originalCurrency,
                originalAmount,
                newBaseAmount);
    }

    /**
     * O valor fora do real, para exibir ao lado do valor em reais: a operação feita em outra moeda
     * ou, sem ela, o valor na moeda da conta estrangeira. {@code null} quando tudo foi em reais.
     */
    public Money foreignValue(Currency accountCurrency) {
        if (originalCurrency != null) {
            return new Money(originalCurrency, originalAmount);
        }
        return accountCurrency.isBase() ? null : new Money(accountCurrency, amount);
    }

    public boolean isPaid() {
        return status == TransactionStatus.PAID;
    }

    /**
     * Ocorrência de um {@link RecurringTransaction} lançada na data {@code occurrenceDate}. Nasce
     * pendente: o sistema lança no dia, mas quem confirma que o dinheiro entrou/saiu é o usuário.
     */
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
                recurrence.id(),
                TransactionStatus.PENDING);
    }
}
