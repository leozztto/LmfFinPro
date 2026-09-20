package com.lmf.finpro.domain.model;

import java.math.BigDecimal;

public record DashboardOverview(
    BigDecimal currentBalance,
    BigDecimal currentMonthIncome,
    BigDecimal currentMonthExpense,
    Double balanceDeltaPercent,
    Double incomeDeltaPercent,
    Double expenseDeltaPercent
) {
}
