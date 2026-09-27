package com.lmf.finpro.infrastructure.web.dto.networth;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lmf.finpro.domain.model.AccountScope;
import com.lmf.finpro.domain.model.DebtType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * @param current contas + investimentos − dívidas agora
 * @param investmentGain rendimento somado de todos os investimentos (valor atual − aplicado)
 * @param changeFromPreviousMonth patrimônio atual − o do fim do mês anterior ({@code null} com um
 *     mês só)
 * @param history um ponto por mês, terminando no atual
 */
public record NetWorthResponse(
        PointRow current,
        BigDecimal changeFromPreviousMonth,
        BigDecimal investmentGain,
        List<PointRow> history,
        List<AccountRow> accounts,
        List<InvestmentRow> investments,
        List<DebtRow> debts) {

    public record PointRow(
            @JsonFormat(pattern = "yyyy-MM") YearMonth month,
            BigDecimal cash,
            BigDecimal investments,
            BigDecimal debts,
            BigDecimal netWorth) {}

    public record AccountRow(Long accountId, String name, AccountScope scope, BigDecimal balance) {}

    /**
     * @param invested saldo inicial + entradas − saídas (aplicado líquido de resgates)
     * @param gain valor atual − aplicado
     * @param gainRate fração (0.05 = 5%); {@code null} se o aplicado não é positivo
     */
    public record InvestmentRow(
            Long accountId,
            String name,
            AccountScope scope,
            BigDecimal invested,
            BigDecimal currentValue,
            BigDecimal gain,
            BigDecimal gainRate,
            LocalDate lastValuationDate) {}

    public record DebtRow(
            Long debtId,
            String name,
            DebtType type,
            String creditor,
            BigDecimal currentBalance,
            LocalDate lastBalanceDate) {}
}
