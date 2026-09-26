package com.lmf.finpro.domain.model;

import java.math.BigDecimal;

/**
 * {@code currentBalance} considera só transações pagas; {@code projectedBalance} = saldo atual +
 * {@code pendingIncome} (a receber) - {@code pendingExpense} (a pagar).
 */
public record DashboardOverview(
        BigDecimal currentBalance,
        BigDecimal currentMonthIncome,
        BigDecimal currentMonthExpense,
        Double balanceDeltaPercent,
        Double incomeDeltaPercent,
        Double expenseDeltaPercent,
        BigDecimal pendingIncome,
        BigDecimal pendingExpense,
        BigDecimal projectedBalance) {}
