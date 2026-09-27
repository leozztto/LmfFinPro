package com.lmf.finpro.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Patrimônio líquido — puro, sem acesso a repositório: contas (caixa) + investimentos (valor de
 * mercado) − dívidas (último saldo devedor), no fim de cada mês pedido.
 *
 * <p>O mês atual usa os valores de agora, com a mesma regra do saldo da tela de Contas (todas as
 * transações pagas, qualquer que seja a data), para o último ponto do gráfico bater com os cards.
 */
public final class NetWorthCalculator {

    private static final int RATE_SCALE = 4;

    private NetWorthCalculator() {}

    public record Point(
            YearMonth month,
            BigDecimal cash,
            BigDecimal investments,
            BigDecimal debts,
            BigDecimal netWorth) {}

    public record AccountRow(Account account, BigDecimal balance) {}

    /**
     * @param invested saldo contábil: saldo inicial + entradas − saídas (o que foi aplicado,
     *     líquido de resgates)
     * @param gain valor atual − aplicado (rendimento ainda não resgatado)
     * @param gainRate rendimento ÷ aplicado; {@code null} quando o aplicado não é positivo
     * @param lastValuation última valorização informada, ou {@code null}
     */
    public record InvestmentRow(
            Account account,
            BigDecimal invested,
            BigDecimal currentValue,
            BigDecimal gain,
            BigDecimal gainRate,
            AccountValuation lastValuation) {}

    /**
     * @param lastBalance último saldo devedor informado, ou {@code null}
     */
    public record DebtRow(Debt debt, BigDecimal currentBalance, DebtBalance lastBalance) {}

    /**
     * @param investmentGain soma do rendimento de todos os investimentos
     * @param changeFromPreviousMonth patrimônio atual − o do fim do mês anterior; {@code null} com
     *     histórico de um mês só
     * @param history um ponto por mês, na ordem pedida; o último é o atual
     * @param accounts contas que não são de investimento, com o saldo atual
     */
    public record Report(
            Point current,
            BigDecimal changeFromPreviousMonth,
            BigDecimal investmentGain,
            List<Point> history,
            List<AccountRow> accounts,
            List<InvestmentRow> investments,
            List<DebtRow> debts) {}

    /**
     * @param transactions transações do usuário, de todas as contas (pagas e pendentes; só as pagas
     *     contam)
     * @param months meses do histórico em ordem, terminando no mês atual
     */
    public static Report calculate(
            List<Account> accounts,
            List<Transaction> transactions,
            List<AccountValuation> valuations,
            List<Debt> debts,
            List<DebtBalance> debtBalances,
            List<YearMonth> months,
            YearMonth currentMonth) {
        Map<Long, List<Transaction>> transactionsByAccount =
                transactions.stream().collect(Collectors.groupingBy(Transaction::accountId));
        Map<Long, List<DebtBalance>> balancesByDebt =
                debtBalances.stream().collect(Collectors.groupingBy(DebtBalance::debtId));

        List<Point> history = new ArrayList<>();
        for (YearMonth month : months) {
            LocalDate asOf = month.equals(currentMonth) ? null : month.atEndOfMonth();
            BigDecimal cash = BigDecimal.ZERO;
            BigDecimal investments = BigDecimal.ZERO;
            for (Account account : accounts) {
                BigDecimal balance =
                        AccountBalances.balance(
                                account,
                                transactionsByAccount.getOrDefault(account.id(), List.of()),
                                valuations,
                                asOf);
                if (account.type() == AccountType.INVESTMENT) {
                    investments = investments.add(balance);
                } else {
                    cash = cash.add(balance);
                }
            }
            BigDecimal debtTotal = BigDecimal.ZERO;
            for (Debt debt : debts) {
                debtTotal =
                        debtTotal.add(
                                latestBalance(
                                                balancesByDebt.getOrDefault(debt.id(), List.of()),
                                                asOf)
                                        .map(DebtBalance::balance)
                                        .orElse(BigDecimal.ZERO));
            }
            history.add(
                    new Point(
                            month,
                            cash,
                            investments,
                            debtTotal,
                            cash.add(investments).subtract(debtTotal)));
        }

        List<AccountRow> accountRows = new ArrayList<>();
        List<InvestmentRow> investmentRows = new ArrayList<>();
        for (Account account : accounts) {
            List<Transaction> accountTransactions =
                    transactionsByAccount.getOrDefault(account.id(), List.of());
            BigDecimal current =
                    AccountBalances.balance(account, accountTransactions, valuations, null);
            if (account.type() != AccountType.INVESTMENT) {
                accountRows.add(new AccountRow(account, current));
                continue;
            }
            BigDecimal invested = AccountBalances.bookBalance(account, accountTransactions, null);
            BigDecimal gain = current.subtract(invested);
            investmentRows.add(
                    new InvestmentRow(
                            account,
                            invested,
                            current,
                            gain,
                            invested.signum() > 0
                                    ? gain.divide(invested, RATE_SCALE, RoundingMode.HALF_UP)
                                    : null,
                            AccountBalances.latestValuation(account, valuations, null)
                                    .orElse(null)));
        }
        accountRows.sort(Comparator.comparing(AccountRow::balance).reversed());
        investmentRows.sort(Comparator.comparing(InvestmentRow::currentValue).reversed());

        List<DebtRow> debtRows =
                debts.stream()
                        .map(
                                debt -> {
                                    DebtBalance last =
                                            latestBalance(
                                                            balancesByDebt.getOrDefault(
                                                                    debt.id(), List.of()),
                                                            null)
                                                    .orElse(null);
                                    return new DebtRow(
                                            debt,
                                            last == null ? BigDecimal.ZERO : last.balance(),
                                            last);
                                })
                        .sorted(Comparator.comparing(DebtRow::currentBalance).reversed())
                        .toList();

        Point current = history.get(history.size() - 1);
        BigDecimal changeFromPreviousMonth =
                history.size() < 2
                        ? null
                        : current.netWorth().subtract(history.get(history.size() - 2).netWorth());
        return new Report(
                current,
                changeFromPreviousMonth,
                investmentRows.stream()
                        .map(InvestmentRow::gain)
                        .reduce(BigDecimal.ZERO, BigDecimal::add),
                history,
                accountRows,
                investmentRows,
                debtRows);
    }

    /** Último saldo devedor com data até {@code asOf} (qualquer um, se nulo). */
    public static Optional<DebtBalance> latestBalance(List<DebtBalance> balances, LocalDate asOf) {
        return balances.stream()
                .filter(balance -> asOf == null || !balance.balanceDate().isAfter(asOf))
                .max(Comparator.comparing(DebtBalance::balanceDate));
    }
}
