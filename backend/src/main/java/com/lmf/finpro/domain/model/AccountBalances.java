package com.lmf.finpro.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Saldo de uma conta numa data — puro, sem acesso a repositório. Só transações pagas contam
 * (transferências inclusive, porque movem dinheiro entre contas).
 *
 * <p>Na conta de investimento, o saldo segue o valor de mercado: o último {@link AccountValuation}
 * até a data mais as movimentações posteriores a ele. Sem valorização informada, vale o saldo
 * contábil, como nas demais contas. O valor informado inclui o que foi lançado até então com data
 * até o dia dele; no mesmo dia, o que foi lançado depois conta à parte.
 *
 * <p>Os métodos recebem só as transações da própria conta.
 */
public final class AccountBalances {

    private AccountBalances() {}

    /**
     * Saldo inicial + receitas − despesas pagas com data até {@code asOf}. Com {@code asOf} nulo,
     * todas as pagas, qualquer que seja a data (o saldo atual da tela de Contas).
     */
    public static BigDecimal bookBalance(
            Account account, List<Transaction> transactions, LocalDate asOf) {
        return account.initialBalance().add(netFlow(transactions, null, asOf));
    }

    /** Saldo considerando o valor de mercado nas contas de investimento. */
    public static BigDecimal balance(
            Account account,
            List<Transaction> transactions,
            List<AccountValuation> valuations,
            LocalDate asOf) {
        Optional<AccountValuation> latest = latestValuation(account, valuations, asOf);
        if (latest.isEmpty()) {
            return bookBalance(account, transactions, asOf);
        }
        AccountValuation valuation = latest.get();
        return valuation.value().add(netFlow(transactions, valuation, asOf));
    }

    /**
     * Rendimento ainda não realizado até {@code asOf}: valor de mercado − saldo contábil. Zero nas
     * contas que não são de investimento ou sem valorização informada até a data.
     */
    public static BigDecimal valuationGain(
            Account account,
            List<Transaction> transactions,
            List<AccountValuation> valuations,
            LocalDate asOf) {
        return balance(account, transactions, valuations, asOf)
                .subtract(bookBalance(account, transactions, asOf));
    }

    /**
     * Última valorização da conta com data até {@code asOf} (qualquer uma, se nulo). Só a conta de
     * investimento usa valorização.
     */
    public static Optional<AccountValuation> latestValuation(
            Account account, List<AccountValuation> valuations, LocalDate asOf) {
        if (account.type() != AccountType.INVESTMENT) {
            return Optional.empty();
        }
        return valuations.stream()
                .filter(valuation -> valuation.accountId().equals(account.id()))
                .filter(valuation -> asOf == null || !valuation.valuationDate().isAfter(asOf))
                .max(Comparator.comparing(AccountValuation::valuationDate));
    }

    /**
     * Receitas − despesas pagas da conta posteriores à valorização {@code after} e com data até
     * {@code until}; nulos = sem limite.
     */
    private static BigDecimal netFlow(
            List<Transaction> transactions, AccountValuation after, LocalDate until) {
        BigDecimal net = BigDecimal.ZERO;
        for (Transaction transaction : transactions) {
            if (!transaction.isPaid()
                    || (after != null && !isAfter(transaction, after))
                    || (until != null && transaction.transactionDate().isAfter(until))) {
                continue;
            }
            net =
                    transaction.type() == CategoryType.INCOME
                            ? net.add(transaction.amount())
                            : net.subtract(transaction.amount());
        }
        return net;
    }

    /**
     * Movimentação de data posterior à da valorização, ou do mesmo dia mas lançada depois de o
     * valor ser informado (ex.: o resgate feito logo após conferir o extrato). As do mesmo dia
     * lançadas antes já estão dentro do valor informado.
     */
    private static boolean isAfter(Transaction transaction, AccountValuation valuation) {
        LocalDate date = transaction.transactionDate();
        if (!date.isEqual(valuation.valuationDate())) {
            return date.isAfter(valuation.valuationDate());
        }
        return transaction.createdAt() != null
                && valuation.createdAt() != null
                && transaction.createdAt().isAfter(valuation.createdAt());
    }
}
