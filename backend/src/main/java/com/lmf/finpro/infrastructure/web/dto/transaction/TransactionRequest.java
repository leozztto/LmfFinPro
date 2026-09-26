package com.lmf.finpro.infrastructure.web.dto.transaction;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.TransactionStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * {@code status} é opcional: na criação, ausente usa o padrão pela data (futura = pendente); na
 * edição, ausente mantém a situação atual.
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
        TransactionStatus status) {

    @JsonCreator
    public TransactionRequest {}

    /** Sem situação informada. */
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
