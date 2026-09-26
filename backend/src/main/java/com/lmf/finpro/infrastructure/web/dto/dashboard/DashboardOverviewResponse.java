package com.lmf.finpro.infrastructure.web.dto.dashboard;

import java.math.BigDecimal;

public record DashboardOverviewResponse(
        BigDecimal currentBalance,
        BigDecimal currentMonthIncome,
        BigDecimal currentMonthExpense,
        Double balanceDeltaPercent,
        Double incomeDeltaPercent,
        Double expenseDeltaPercent,
        BigDecimal pendingIncome,
        BigDecimal pendingExpense,
        BigDecimal projectedBalance) {}
