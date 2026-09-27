package com.lmf.finpro.infrastructure.web.dto.recurringtransaction;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.RecurrenceFrequency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** {@code tagNames} opcional: vale para todas as transações que a recorrência gerar. */
public record RecurringTransactionRequest(
        @NotNull(message = "conta é obrigatória") Long accountId,
        Long categoryId,
        Long clientId,
        @NotBlank(message = "descrição é obrigatória") String description,
        @NotNull(message = "valor é obrigatório")
                @DecimalMin(value = "0.01", message = "valor deve ser maior que zero")
                BigDecimal amount,
        @NotNull(message = "tipo é obrigatório") CategoryType type,
        @NotNull(message = "frequência é obrigatória") RecurrenceFrequency frequency,
        @NotNull(message = "data inicial é obrigatória") LocalDate startDate,
        LocalDate endDate,
        List<String> tagNames) {

    @JsonCreator
    public RecurringTransactionRequest {}

    /** Sem tags. */
    public RecurringTransactionRequest(
            Long accountId,
            Long categoryId,
            Long clientId,
            String description,
            BigDecimal amount,
            CategoryType type,
            RecurrenceFrequency frequency,
            LocalDate startDate,
            LocalDate endDate) {
        this(
                accountId,
                categoryId,
                clientId,
                description,
                amount,
                type,
                frequency,
                startDate,
                endDate,
                null);
    }
}
