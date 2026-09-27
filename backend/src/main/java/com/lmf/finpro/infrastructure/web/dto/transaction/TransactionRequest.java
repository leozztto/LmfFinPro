package com.lmf.finpro.infrastructure.web.dto.transaction;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.Currency;
import com.lmf.finpro.domain.model.TransactionStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * {@code status} é opcional: na criação, ausente usa o padrão pela data (futura = pendente); na
 * edição, ausente mantém a situação atual. {@code tagNames} também: na criação, ausente = sem tags;
 * na edição, ausente mantém as tags atuais. Tag nova é criada na hora. {@code amount} é o valor na
 * moeda da conta; {@code originalCurrency}/{@code originalAmount}, a moeda e o valor da operação
 * quando feita em outra moeda (ex.: compra em dólar no cartão em reais).
 */
public record TransactionRequest(
        Long accountId,
        Long categoryId,
        Long clientId,
        @NotBlank(message = "descrição é obrigatória") String description,
        @NotNull(message = "valor é obrigatório")
                @DecimalMin(value = "0.01", message = "valor deve ser maior que zero")
                BigDecimal amount,
        @NotNull(message = "data é obrigatória") LocalDate transactionDate,
        @NotNull(message = "tipo é obrigatório") CategoryType type,
        TransactionStatus status,
        List<String> tagNames,
        Currency originalCurrency,
        @DecimalMin(value = "0.01", message = "valor na moeda da operação deve ser maior que zero")
                BigDecimal originalAmount) {

    @JsonCreator
    public TransactionRequest {}

    /** Operação na moeda da conta. */
    public TransactionRequest(
            Long accountId,
            Long categoryId,
            Long clientId,
            String description,
            BigDecimal amount,
            LocalDate transactionDate,
            CategoryType type,
            TransactionStatus status,
            List<String> tagNames) {
        this(
                accountId,
                categoryId,
                clientId,
                description,
                amount,
                transactionDate,
                type,
                status,
                tagNames,
                null,
                null);
    }

    /** Sem tags informadas. */
    public TransactionRequest(
            Long accountId,
            Long categoryId,
            Long clientId,
            String description,
            BigDecimal amount,
            LocalDate transactionDate,
            CategoryType type,
            TransactionStatus status) {
        this(
                accountId,
                categoryId,
                clientId,
                description,
                amount,
                transactionDate,
                type,
                status,
                null);
    }

    /** Sem situação nem tags informadas. */
    public TransactionRequest(
            Long accountId,
            Long categoryId,
            Long clientId,
            String description,
            BigDecimal amount,
            LocalDate transactionDate,
            CategoryType type) {
        this(accountId, categoryId, clientId, description, amount, transactionDate, type, null);
    }
}
