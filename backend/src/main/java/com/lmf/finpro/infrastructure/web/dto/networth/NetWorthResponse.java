package com.lmf.finpro.infrastructure.web.dto.networth;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lmf.finpro.domain.model.AccountScope;
import com.lmf.finpro.domain.model.Currency;
import com.lmf.finpro.domain.model.DebtType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * Valores consolidados em reais; as linhas de conta e investimento trazem também a moeda da conta.
 *
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

    /**
     * @param balance saldo na moeda da conta
     * @param balanceInBrl saldo em reais, pela última cotação
     */
    public record AccountRow(
            Long accountId,
            String name,
            AccountScope scope,
            BigDecimal balance,
            Currency currency,
            BigDecimal balanceInBrl) {}

    /**
     * Valores na moeda da conta; {@code currentValueInBrl} e {@code gainInBrl} em reais.
     *
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
            LocalDate lastValuationDate,
            Currency currency,
            BigDecimal currentValueInBrl,
            BigDecimal gainInBrl) {}

    public record DebtRow(
            Long debtId,
            String name,
            DebtType type,
            String creditor,
            BigDecimal currentBalance,
            LocalDate lastBalanceDate) {}
}
