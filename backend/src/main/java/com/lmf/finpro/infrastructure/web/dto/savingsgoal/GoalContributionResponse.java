package com.lmf.finpro.infrastructure.web.dto.savingsgoal;

import com.lmf.finpro.domain.model.ContributionType;
import java.math.BigDecimal;
import java.time.LocalDate;

public record GoalContributionResponse(
        Long id,
        Long goalId,
        ContributionType type,
        BigDecimal amount,
        LocalDate contributionDate,
        String note,
        Long transferId) {}
