package com.lmf.finpro.domain.model;

import com.lmf.finpro.domain.model.PayrollTaxCalculator.PayrollTaxes;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Conta do "quanto posso me pagar este mês" — pura, sem acesso a repositório. Em duas etapas:
 *
 * <p><b>1. Orçamento do pró-labore no mês</b> — quanto a empresa pode gastar com o sócio no mês
 * (bruto + INSS patronal), conforme a base:
 *
 * <ul>
 *   <li><b>Receitas do mês</b>: receitas PJ recebidas − despesas PJ do mês − imposto − reserva,
 *       limitado ao que o saldo PJ comporta (saldo − contas a pagar − imposto + o que já foi
 *       retirado no mês, que saiu do saldo mas faz parte do pró-labore do mês).
 *   <li><b>Saldo atual</b>: saldo PJ − contas a pagar até o fim do mês − imposto a reservar −
 *       colchão de caixa + o já retirado no mês (pelo mesmo motivo).
 * </ul>
 *
 * <p><b>2. Encargos</b> — com retenção ligada, o bruto é o orçamento ÷ (1 + INSS patronal); dele
 * saem o INSS do sócio e o IRRF ({@link PayrollTaxCalculator}), e o que sobra é o líquido que vai
 * para a conta PF. Sem retenção, bruto = líquido = orçamento.
 *
 * <p>O disponível para transferir é o líquido do mês menos o que já foi retirado (as retiradas são
 * transferências PJ → PF, ou seja, valores líquidos). Com pró-labore fixo, os encargos e o valor
 * sugerido para pagar partem do fixo (limitado ao orçamento).
 */
public final class ProLaboreCalculator {

    private ProLaboreCalculator() {}

    /**
     * @param monthIncome receitas PJ recebidas (pagas) no mês
     * @param monthExpenses despesas PJ do mês: pagas no mês e pendentes até o fim dele
     * @param pendingExpenses despesas PJ pendentes até o fim do mês (inclusive atrasadas)
     * @param taxReserveSaved valor guardado nas caixinhas do imposto
     * @param withdrawnThisMonth transferências PJ → PF no mês (valores líquidos)
     * @param withholdingApplied se desconta INSS e IRRF (já resolvido pelo regime)
     * @param employerInssRate INSS patronal efetivo (zero sem retenção)
     */
    public record Inputs(
            ProLaboreSettings settings,
            BigDecimal businessBalance,
            BigDecimal monthIncome,
            BigDecimal monthExpenses,
            BigDecimal pendingExpenses,
            BigDecimal taxRate,
            BigDecimal taxReserveSaved,
            BigDecimal averageMonthlyExpense,
            BigDecimal withdrawnThisMonth,
            boolean withholdingApplied,
            BigDecimal employerInssRate) {}

    /**
     * @param calculatedAmount orçamento do mês antes do limite de saldo (pode ser negativo)
     * @param balanceCap teto pelo saldo na base de receitas; null na base de saldo
     * @param monthBudget orçamento do mês (bruto + INSS patronal), nunca negativo
     * @param payroll bruto, encargos e líquido do pró-labore do mês (o fixo, se houver)
     * @param maxNet líquido máximo do mês, se todo o orçamento fosse para o pró-labore
     * @param availableToWithdraw líquido máximo − já retirado
     * @param fixedRemaining líquido do fixo − já retirado; null sem fixo
     * @param fixedCovered se o orçamento do mês comporta o fixo (bruto + patronal)
     * @param suggestedPayment valor da transferência sugerida: o líquido planejado − já retirado
     */
    public record Result(
            BigDecimal taxOnMonthIncome,
            BigDecimal taxReserve,
            BigDecimal reserve,
            BigDecimal cashCushion,
            BigDecimal calculatedAmount,
            BigDecimal balanceCap,
            boolean cappedByBalance,
            BigDecimal monthBudget,
            boolean withholdingApplied,
            BigDecimal employerInssRate,
            PayrollTaxes payroll,
            BigDecimal maxNet,
            BigDecimal availableToWithdraw,
            BigDecimal fixedRemaining,
            boolean fixedCovered,
            BigDecimal suggestedPayment) {}

    public static Result calculate(Inputs in) {
        ProLaboreSettings settings = in.settings();
        BigDecimal withdrawn = in.withdrawnThisMonth();
        BigDecimal taxOnMonthIncome = money(in.monthIncome().multiply(in.taxRate()));

        BigDecimal taxReserve;
        BigDecimal reserve = BigDecimal.ZERO;
        BigDecimal cashCushion = BigDecimal.ZERO;
        BigDecimal calculated;
        BigDecimal balanceCap = null;
        BigDecimal budget;

        if (settings.calculationBase() == ProLaboreCalculationBase.MONTH_INCOME) {
            taxReserve = taxOnMonthIncome;
            reserve = money(in.monthIncome().multiply(settings.reserveRate()));
            calculated =
                    in.monthIncome()
                            .subtract(in.monthExpenses())
                            .subtract(taxReserve)
                            .subtract(reserve);
            balanceCap =
                    in.businessBalance()
                            .subtract(in.pendingExpenses())
                            .subtract(taxReserve)
                            .add(withdrawn)
                            .max(BigDecimal.ZERO);
            budget = calculated.min(balanceCap).max(BigDecimal.ZERO);
        } else {
            // A caixinha é virtual: o que já está nela continua no saldo e não pode ser retirado.
            taxReserve = taxOnMonthIncome.max(in.taxReserveSaved());
            cashCushion =
                    money(
                            in.averageMonthlyExpense()
                                    .multiply(BigDecimal.valueOf(settings.cashCushionMonths())));
            calculated =
                    in.businessBalance()
                            .subtract(in.pendingExpenses())
                            .subtract(taxReserve)
                            .subtract(cashCushion)
                            .add(withdrawn);
            budget = calculated.max(BigDecimal.ZERO);
        }
        boolean cappedByBalance =
                balanceCap != null
                        && calculated.signum() > 0
                        && calculated.compareTo(balanceCap) > 0;

        BigDecimal employerRate = in.withholdingApplied() ? in.employerInssRate() : BigDecimal.ZERO;
        BigDecimal maxGross = budget.divide(BigDecimal.ONE.add(employerRate), 2, RoundingMode.DOWN);
        BigDecimal fixed = settings.fixedAmount();
        BigDecimal plannedGross = fixed == null ? maxGross : fixed.min(maxGross);

        PayrollTaxes payroll = payroll(plannedGross, in.withholdingApplied(), employerRate);
        BigDecimal maxNet = payroll(maxGross, in.withholdingApplied(), employerRate).net();
        BigDecimal available = maxNet.subtract(withdrawn).max(BigDecimal.ZERO);
        BigDecimal suggestedPayment = payroll.net().subtract(withdrawn).max(BigDecimal.ZERO);

        BigDecimal fixedRemaining = null;
        boolean fixedCovered = false;
        if (fixed != null) {
            BigDecimal fixedNet = payroll(fixed, in.withholdingApplied(), employerRate).net();
            fixedRemaining = fixedNet.subtract(withdrawn).max(BigDecimal.ZERO);
            fixedCovered = fixed.compareTo(maxGross) <= 0;
        }

        return new Result(
                taxOnMonthIncome,
                taxReserve,
                reserve,
                cashCushion,
                calculated,
                balanceCap,
                cappedByBalance,
                budget,
                in.withholdingApplied(),
                employerRate,
                payroll,
                maxNet,
                available,
                fixedRemaining,
                fixedCovered,
                suggestedPayment);
    }

    private static PayrollTaxes payroll(
            BigDecimal gross, boolean withholding, BigDecimal employerRate) {
        return withholding
                ? PayrollTaxCalculator.compute(gross, employerRate)
                : PayrollTaxCalculator.none(gross);
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
