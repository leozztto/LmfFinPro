package com.lmf.finpro.infrastructure.web.dto.prolabore;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * @param monthBudget quanto a empresa pode gastar com o pró-labore no mês (bruto + INSS patronal)
 * @param availableToWithdraw líquido máximo do mês − já retirado: quanto ainda dá para transferir
 * @param suggestedPayment transferência sugerida (o que falta do fixo, se houver)
 * @param payroll bruto, encargos e líquido do pró-labore do mês
 */
public record ProLaboreResponse(
        boolean hasBusinessAccounts,
        boolean hasPersonalAccounts,
        SettingsResponse settings,
        BigDecimal availableToWithdraw,
        BigDecimal suggestedPayment,
        BigDecimal calculatedAmount,
        BigDecimal balanceCap,
        boolean cappedByBalance,
        BigDecimal monthBudget,
        BigDecimal businessBalance,
        BigDecimal monthBusinessIncome,
        BigDecimal monthBusinessExpenses,
        BigDecimal pendingBusinessExpenses,
        BigDecimal taxRate,
        BigDecimal taxOnMonthIncome,
        BigDecimal taxReserveSaved,
        BigDecimal taxReserve,
        BigDecimal reserve,
        BigDecimal averageMonthlyBusinessExpense,
        BigDecimal cashCushion,
        boolean withholdingApplied,
        BigDecimal employerInssRate,
        PayrollResponse payroll,
        BigDecimal fixedRemaining,
        boolean fixedCovered,
        BigDecimal withdrawnThisMonth,
        List<WithdrawalResponse> withdrawals,
        Long suggestedFromAccountId,
        Long suggestedToAccountId) {

    public record SettingsResponse(
            String calculationBase,
            int cashCushionMonths,
            BigDecimal reserveRate,
            String taxMode,
            BigDecimal manualTaxRate,
            BigDecimal fixedAmount,
            String withholdingMode,
            BigDecimal employerInssRate) {}

    /**
     * @param taxesToCollect guias a pagar pela empresa: INSS do sócio + IRRF + INSS patronal
     */
    public record PayrollResponse(
            BigDecimal gross,
            BigDecimal employeeInss,
            BigDecimal irrf,
            BigDecimal employerInss,
            BigDecimal net,
            BigDecimal taxesToCollect) {}

    public record WithdrawalResponse(
            Long transferId,
            LocalDate date,
            BigDecimal amount,
            String fromAccountName,
            String toAccountName) {}
}
