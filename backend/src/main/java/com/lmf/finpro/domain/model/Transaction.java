package com.lmf.finpro.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * @param amount valor na moeda da conta — é o que mexe no saldo dela
 * @param originalCurrency moeda em que a operação foi feita, quando diferente da moeda da conta
 *     (ex.: compra em dólar no cartão em reais); {@code null} quando é a mesma
 * @param originalAmount valor na {@code originalCurrency}; {@code null} junto com ela
 * @param baseAmount valor em reais, usado em todo total que junta contas (dashboard, relatórios,
 *     orçamentos). Na conta em reais é o próprio {@code amount}; nas demais, a conversão pela
 *     cotação do dia da transação, gravada ao salvar
 * @param transactionTime hora da transação, quando o extrato importado (CSV ou OFX) trouxer esse
 *     dado; {@code null} em lançamentos manuais, transferências, recorrências e importações sem
 *     hora. Usado hoje só para detectar duplicidade ao reimportar um extrato — não é editável pela
 *     tela. Fica ao lado de {@code transactionDate} (que continua {@code LocalDate}) em vez de
 *     virar um único campo {@code LocalDateTime}, para não tocar toda a lógica que já agrupa por
 *     mês/dia (orçamento, dashboard, alertas, calendário, relatórios).
 * @param paidAt dia em que a pendência foi marcada como paga; {@code null} em quem nasceu paga ou
 *     já estava paga antes deste campo existir (o atraso, nesses casos, é desconhecido). Base do
 *     insight "cliente que atrasa": paga depois do vencimento ({@code transactionDate}) é atraso.
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
        BigDecimal baseAmount,
        LocalTime transactionTime,
        LocalDate paidAt) {

    /** Transação sem data de pagamento registrada (a maioria: nasce paga ou veio de antes). */
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
            TransactionStatus status,
            Currency originalCurrency,
            BigDecimal originalAmount,
            BigDecimal baseAmount,
            LocalTime transactionTime) {
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
                originalCurrency,
                originalAmount,
                baseAmount,
                transactionTime,
                null);
    }

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
                amount,
                null);
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
            LocalTime transactionTime,
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
                importBatchId,
                null,
                TransactionStatus.PAID,
                null,
                null,
                amount,
                transactionTime);
    }

    /**
     * Novos dados. O valor em reais volta a ser o próprio {@code newAmount}: quem salva a transação
     * de uma conta em outra moeda converte de novo ({@link #withBaseAmount}). A hora (quando veio
     * de uma importação) não muda numa edição manual de categoria/cliente/descrição/valor/data.
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
                newAmount,
                transactionTime,
                paidAt);
    }

    /**
     * Muda a situação registrando o dia em que foi paga: ao passar de pendente para paga, {@code
     * paidAt} vira {@code today}; ao voltar a pendente, é limpo. Quem já estava paga mantém a data
     * que tinha.
     */
    public Transaction withStatus(TransactionStatus newStatus, LocalDate today) {
        LocalDate newPaidAt =
                newStatus == TransactionStatus.PAID
                        ? (status == TransactionStatus.PAID ? paidAt : today)
                        : null;
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
                baseAmount,
                transactionTime,
                newPaidAt);
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
                baseAmount,
                transactionTime,
                paidAt);
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
                baseAmount,
                transactionTime,
                paidAt);
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
                newBaseAmount,
                transactionTime,
                paidAt);
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
