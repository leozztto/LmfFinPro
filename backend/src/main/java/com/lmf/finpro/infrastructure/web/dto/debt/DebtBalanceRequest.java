package com.lmf.finpro.infrastructure.web.dto.debt;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Saldo zero marca a dívida como quitada. */
public record DebtBalanceRequest(
        @NotNull(message = "data é obrigatória") LocalDate balanceDate,
        @NotNull(message = "saldo devedor é obrigatório")
                @DecimalMin(value = "0.0", message = "saldo devedor não pode ser negativo")
                BigDecimal balance) {}
