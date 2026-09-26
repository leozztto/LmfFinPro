package com.lmf.finpro.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Encargos sobre o pró-labore do sócio, com valores de referência de 2026 — atualizar a cada ano.
 * Estimativa para planejamento, não substitui a folha feita pelo contador.
 *
 * <ul>
 *   <li><b>INSS do sócio</b> (contribuinte individual): 11% sobre o pró-labore, limitado ao teto de
 *       R$ 8.475,55.
 *   <li><b>IRRF</b>: tabela progressiva mensal sobre o pró-labore menos a maior dedução entre o
 *       INSS e o desconto simplificado (R$ 607,20); depois, o redutor da Lei 15.270/2025 — imposto
 *       zero até R$ 5.000 de rendimento e redução de {@code 978,62 − 0,133145 × rendimento} até R$
 *       7.350.
 *   <li><b>INSS patronal</b>: alíquota informada (ex.: 20% no Lucro Presumido), paga pela empresa
 *       além do pró-labore.
 * </ul>
 */
public final class PayrollTaxCalculator {

    static final BigDecimal INSS_RATE = new BigDecimal("0.11");
    static final BigDecimal INSS_CEILING = new BigDecimal("8475.55");
    static final BigDecimal SIMPLIFIED_DISCOUNT = new BigDecimal("607.20");
    static final BigDecimal FULL_REDUCTION_LIMIT = new BigDecimal("5000.00");
    static final BigDecimal PARTIAL_REDUCTION_LIMIT = new BigDecimal("7350.00");
    static final BigDecimal REDUCTION_CONSTANT = new BigDecimal("978.62");
    static final BigDecimal REDUCTION_FACTOR = new BigDecimal("0.133145");

    /** {limite superior da base, alíquota, parcela a deduzir}; a última faixa não tem limite. */
    private static final BigDecimal[][] IRRF_TABLE = {
        {new BigDecimal("2428.80"), BigDecimal.ZERO, BigDecimal.ZERO},
        {new BigDecimal("2826.65"), new BigDecimal("0.075"), new BigDecimal("182.16")},
        {new BigDecimal("3751.05"), new BigDecimal("0.15"), new BigDecimal("394.16")},
        {new BigDecimal("4664.68"), new BigDecimal("0.225"), new BigDecimal("675.49")},
        {null, new BigDecimal("0.275"), new BigDecimal("908.73")},
    };

    private PayrollTaxCalculator() {}

    /**
     * @param net o que o sócio recebe: bruto − INSS − IRRF
     * @param employerInss pago pela empresa, além do bruto
     */
    public record PayrollTaxes(
            BigDecimal gross,
            BigDecimal employeeInss,
            BigDecimal irrf,
            BigDecimal employerInss,
            BigDecimal net) {

        /** Custo total para a empresa: bruto + INSS patronal. */
        public BigDecimal companyCost() {
            return gross.add(employerInss);
        }

        /** Guias a recolher: INSS do sócio + IRRF + INSS patronal. */
        public BigDecimal taxesToCollect() {
            return employeeInss.add(irrf).add(employerInss);
        }
    }

    public static PayrollTaxes compute(BigDecimal gross, BigDecimal employerInssRate) {
        BigDecimal inss = employeeInss(gross);
        BigDecimal irrf = irrf(gross, inss);
        BigDecimal employer = money(gross.multiply(employerInssRate));
        return new PayrollTaxes(gross, inss, irrf, employer, gross.subtract(inss).subtract(irrf));
    }

    /** Sem retenção (MEI, autônomo ou desligado na configuração): o líquido é o bruto. */
    public static PayrollTaxes none(BigDecimal gross) {
        return new PayrollTaxes(gross, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, gross);
    }

    public static BigDecimal employeeInss(BigDecimal gross) {
        return money(gross.min(INSS_CEILING).max(BigDecimal.ZERO).multiply(INSS_RATE));
    }

    public static BigDecimal irrf(BigDecimal gross, BigDecimal employeeInss) {
        if (gross.compareTo(FULL_REDUCTION_LIMIT) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal base = gross.subtract(employeeInss.max(SIMPLIFIED_DISCOUNT));
        BigDecimal tax = BigDecimal.ZERO;
        for (BigDecimal[] bracket : IRRF_TABLE) {
            if (bracket[0] == null || base.compareTo(bracket[0]) <= 0) {
                tax = base.multiply(bracket[1]).subtract(bracket[2]).max(BigDecimal.ZERO);
                break;
            }
        }
        if (gross.compareTo(PARTIAL_REDUCTION_LIMIT) <= 0) {
            BigDecimal reduction =
                    REDUCTION_CONSTANT
                            .subtract(REDUCTION_FACTOR.multiply(gross))
                            .max(BigDecimal.ZERO);
            tax = tax.subtract(reduction).max(BigDecimal.ZERO);
        }
        return money(tax);
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
