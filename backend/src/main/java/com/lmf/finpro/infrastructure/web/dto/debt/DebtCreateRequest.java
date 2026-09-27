package com.lmf.finpro.infrastructure.web.dto.debt;

import com.lmf.finpro.domain.model.DebtType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/** A dívida nasce com o saldo devedor atual. */
public record DebtCreateRequest(
        @NotBlank(message = "nome é obrigatório")
                @Size(max = 100, message = "nome deve ter no máximo 100 caracteres")
                String name,
        @NotNull(message = "tipo é obrigatório") DebtType type,
        @Size(max = 100, message = "credor deve ter no máximo 100 caracteres") String creditor,
        @NotNull(message = "saldo devedor é obrigatório")
                @DecimalMin(value = "0.0", message = "saldo devedor não pode ser negativo")
                BigDecimal balance,
        @NotNull(message = "data do saldo é obrigatória") LocalDate balanceDate) {}
