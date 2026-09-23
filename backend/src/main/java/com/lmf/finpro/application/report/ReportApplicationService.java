package com.lmf.finpro.application.report;

import com.lmf.finpro.domain.exception.ResourceNotFoundException;
import com.lmf.finpro.domain.model.Account;
import com.lmf.finpro.domain.model.AccountStatementData;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.Client;
import com.lmf.finpro.domain.model.ClientReceiptData;
import com.lmf.finpro.domain.model.Transaction;
import com.lmf.finpro.domain.model.User;
import com.lmf.finpro.domain.port.out.AccountRepositoryPort;
import com.lmf.finpro.domain.port.out.ClientRepositoryPort;
import com.lmf.finpro.domain.port.out.ReceiptGeneratorPort;
import com.lmf.finpro.domain.port.out.TransactionRepositoryPort;
import com.lmf.finpro.domain.port.out.UserRepositoryPort;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReportApplicationService {

    private final ClientRepositoryPort clientRepositoryPort;
    private final AccountRepositoryPort accountRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;
    private final TransactionRepositoryPort transactionRepositoryPort;
    private final ReceiptGeneratorPort receiptGeneratorPort;

    public byte[] generateClientReceipt(
            Long currentUserId, Long clientId, YearMonth referenceMonth) {
        Client client = findOwnedClientOrThrow(currentUserId, clientId);
        User issuer = findUserOrThrow(currentUserId);

        LocalDate start = referenceMonth.atDay(1);
        LocalDate end = referenceMonth.plusMonths(1).atDay(1);
        List<Transaction> transactions =
                transactionRepositoryPort.findAllByClientIdAndTypeAndDateBetween(
                        clientId, CategoryType.INCOME, start, end);

        BigDecimal total =
                transactions.stream()
                        .map(Transaction::amount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        return receiptGeneratorPort.generateClientReceipt(
                new ClientReceiptData(issuer, client, referenceMonth, transactions, total));
    }

    /**
     * Saldo de abertura = saldo inicial da conta + tudo lançado antes do período; saldo de
     * fechamento = abertura + receitas - despesas do período. Reaproveita {@code
     * findAllByAccountIds} (já existente) e divide as transações em memória por data, em vez de
     * criar uma query nova só para isso.
     */
    public byte[] generateAccountStatement(
            Long currentUserId, Long accountId, YearMonth referenceMonth) {
        Account account = findOwnedAccountOrThrow(currentUserId, accountId);
        User issuer = findUserOrThrow(currentUserId);

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

        return receiptGeneratorPort.generateAccountStatement(
                new AccountStatementData(
                        issuer,
                        account,
                        referenceMonth,
                        openingBalance,
                        periodTransactions,
                        totalIncome,
                        totalExpense,
                        closingBalance));
    }

    private Client findOwnedClientOrThrow(Long currentUserId, Long clientId) {
        return clientRepositoryPort
                .findById(clientId)
                .filter(candidate -> candidate.belongsTo(currentUserId))
                .orElseThrow(
                        () -> new ResourceNotFoundException("Cliente não encontrado: " + clientId));
    }

    private Account findOwnedAccountOrThrow(Long currentUserId, Long accountId) {
        return accountRepositoryPort
                .findById(accountId)
                .filter(candidate -> candidate.belongsTo(currentUserId))
                .orElseThrow(
                        () -> new ResourceNotFoundException("Conta não encontrada: " + accountId));
    }

    private User findUserOrThrow(Long currentUserId) {
        return userRepositoryPort
                .findById(currentUserId)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Usuário não encontrado: " + currentUserId));
    }
}
