package com.lmf.finpro.infrastructure.web.dto.recurringtransaction;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Conta, tipo, frequência e data inicial não são editáveis depois de criada a recorrência. */
public record RecurringTransactionUpdateRequest(
        Long categoryId,
        Long clientId,
        @NotBlank(message = "descrição é obrigatória") String description,
        @NotNull(message = "valor é obrigatório")
                @DecimalMin(value = "0.01", message = "valor deve ser maior que zero")
                BigDecimal amount,
        LocalDate endDate,
        @NotNull(message = "situação é obrigatória") Boolean active) {}
