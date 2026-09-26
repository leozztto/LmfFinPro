package com.lmf.finpro.infrastructure.web.dto.savingsgoal;

import com.lmf.finpro.domain.model.SavingsGoalType;
import java.math.BigDecimal;
import java.time.LocalDate;

public record SavingsGoalResponse(
        Long id,
        String name,
        SavingsGoalType type,
        BigDecimal targetAmount,
        LocalDate deadline,
        BigDecimal incomeRate,
        BigDecimal savedAmount,
        BigDecimal remainingAmount,
        BigDecimal monthlyNeeded,
        BigDecimal monthPaidIncome,
        BigDecimal suggestedContribution) {}
