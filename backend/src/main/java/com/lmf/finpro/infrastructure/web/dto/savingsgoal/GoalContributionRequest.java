package com.lmf.finpro.infrastructure.web.dto.savingsgoal;

import com.lmf.finpro.domain.model.ContributionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record GoalContributionRequest(
        @NotNull(message = "tipo é obrigatório") ContributionType type,
        @NotNull(message = "valor é obrigatório")
                @DecimalMin(
                        value = "0.0",
                        inclusive = false,
                        message = "valor deve ser maior que zero")
                BigDecimal amount,
        @NotNull(message = "data é obrigatória") LocalDate contributionDate,
        @Size(max = 255, message = "observação deve ter no máximo 255 caracteres") String note) {}
