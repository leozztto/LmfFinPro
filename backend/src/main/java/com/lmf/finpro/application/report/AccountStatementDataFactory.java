package com.lmf.finpro.application.report;

import com.lmf.finpro.domain.model.Account;
import com.lmf.finpro.domain.model.AccountStatementData;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.Transaction;
import com.lmf.finpro.domain.model.User;
import com.lmf.finpro.domain.port.out.TransactionRepositoryPort;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Monta os dados do extrato de uma conta no mês. */
@Component
@RequiredArgsConstructor
class AccountStatementDataFactory {

    private final ReportLookups lookups;
    private final TransactionRepositoryPort transactionRepositoryPort;

    /**
     * Saldo de abertura = saldo inicial da conta + tudo lançado antes do período; saldo de
     * fechamento = abertura + receitas - despesas do período. Reaproveita {@code
     * findAllByAccountIds} (já existente) e divide as transações em memória por data, em vez de
     * criar uma query nova só para isso.
     */
    AccountStatementData build(Long currentUserId, Long accountId, YearMonth referenceMonth) {
        Account account = lookups.findOwnedAccountOrThrow(currentUserId, accountId);
        User issuer = lookups.findUserOrThrow(currentUserId);

        LocalDate start = referenceMonth.atDay(1);
        LocalDate end = referenceMonth.plusMonths(1).atDay(1);

        List<Transaction> sorted =
                transactionRepositoryPort.findAllByAccountIds(List.of(accountId)).stream()
                        .sorted(Comparator.comparing(Transaction::transactionDate))
                        .toList();

        BigDecimal openingBalance = account.initialBalance();
        List<Transaction> periodTransactions = new ArrayList<>();
        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;

        for (Transaction transaction : sorted) {
            BigDecimal signedAmount =
                    transaction.type() == CategoryType.INCOME
                            ? transaction.amount()
                            : transaction.amount().negate();
            if (transaction.transactionDate().isBefore(start)) {
                openingBalance = openingBalance.add(signedAmount);
            } else if (transaction.transactionDate().isBefore(end)) {
                periodTransactions.add(transaction);
                if (transaction.type() == CategoryType.INCOME) {
                    totalIncome = totalIncome.add(transaction.amount());
                } else {
                    totalExpense = totalExpense.add(transaction.amount());
                }
            }
        }

        BigDecimal closingBalance = openingBalance.add(totalIncome).subtract(totalExpense);

        return new AccountStatementData(
                issuer,
                account,
                referenceMonth,
                openingBalance,
                periodTransactions,
                totalIncome,
                totalExpense,
                closingBalance);
    }
}
