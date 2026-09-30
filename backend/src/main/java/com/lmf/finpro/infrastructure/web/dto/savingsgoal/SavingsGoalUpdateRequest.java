package com.lmf.finpro.infrastructure.web.dto.savingsgoal;

import com.lmf.finpro.domain.model.SavingsGoalType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Conta reserva e conta de origem não são editáveis depois de criada a meta. */
public record SavingsGoalUpdateRequest(
        @NotBlank(message = "nome é obrigatório")
                @Size(max = 100, message = "nome deve ter no máximo 100 caracteres")
                String name,
        @NotNull(message = "tipo é obrigatório") SavingsGoalType type,
        @NotNull(message = "valor-alvo é obrigatório")
                @DecimalMin(
                        value = "0.0",
                        inclusive = false,
                        message = "valor-alvo deve ser maior que zero")
                BigDecimal targetAmount,
        LocalDate deadline,
        @DecimalMin(value = "0.0", message = "percentual não pode ser negativo")
                @DecimalMax(value = "1.0", message = "percentual deve ser no máximo 100%")
                BigDecimal incomeRate,
        boolean autoContribute) {}
