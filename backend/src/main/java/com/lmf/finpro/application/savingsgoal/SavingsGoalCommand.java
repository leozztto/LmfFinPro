package com.lmf.finpro.application.savingsgoal;

import com.lmf.finpro.domain.model.SavingsGoalType;
import java.math.BigDecimal;
import java.time.LocalDate;

public record SavingsGoalCommand(
        String name,
        SavingsGoalType type,
        BigDecimal targetAmount,
        LocalDate deadline,
        BigDecimal incomeRate,
        boolean autoContribute) {}
