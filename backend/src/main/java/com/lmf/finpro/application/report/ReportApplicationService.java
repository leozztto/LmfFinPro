package com.lmf.finpro.application.report;

import com.lmf.finpro.domain.exception.ResourceNotFoundException;
import com.lmf.finpro.domain.model.Account;
import com.lmf.finpro.domain.model.AccountStatementData;
import com.lmf.finpro.domain.model.Category;
import com.lmf.finpro.domain.model.CategoryExpenseReportData;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.Client;
import com.lmf.finpro.domain.model.ClientAnnualStatementData;
import com.lmf.finpro.domain.model.ClientReceiptData;
import com.lmf.finpro.domain.model.Transaction;
import com.lmf.finpro.domain.model.User;
import com.lmf.finpro.domain.port.out.AccountRepositoryPort;
import com.lmf.finpro.domain.port.out.CategoryRepositoryPort;
import com.lmf.finpro.domain.port.out.ClientRepositoryPort;
import com.lmf.finpro.domain.port.out.ReceiptGeneratorPort;
import com.lmf.finpro.domain.port.out.TransactionRepositoryPort;
import com.lmf.finpro.domain.port.out.UserRepositoryPort;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.Year;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReportApplicationService {

    private final ClientRepositoryPort clientRepositoryPort;
    private final AccountRepositoryPort accountRepositoryPort;
    private final CategoryRepositoryPort categoryRepositoryPort;
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

    /**
     * Soma as receitas do cliente em cada um dos 12 meses do ano (mesmo os que ficarem zerados),
     * reaproveitando {@code findAllByClientIdAndTypeAndDateBetween} com o intervalo do ano inteiro
     * em vez de um mês, igual ao recibo mensal.
     */
    public byte[] generateClientAnnualStatement(Long currentUserId, Long clientId, Year year) {
        Client client = findOwnedClientOrThrow(currentUserId, clientId);
        User issuer = findUserOrThrow(currentUserId);

        LocalDate start = year.atDay(1);
        LocalDate end = year.plusYears(1).atDay(1);
        List<Transaction> transactions =
                transactionRepositoryPort.findAllByClientIdAndTypeAndDateBetween(
                        clientId, CategoryType.INCOME, start, end);

        Map<Month, BigDecimal> totalsByMonth = new EnumMap<>(Month.class);
        for (Month month : Month.values()) {
            totalsByMonth.put(month, BigDecimal.ZERO);
        }
        for (Transaction transaction : transactions) {
            totalsByMonth.merge(
                    transaction.transactionDate().getMonth(),
                    transaction.amount(),
                    BigDecimal::add);
        }

        List<ClientAnnualStatementData.MonthlyIncome> monthlyIncomes =
                Arrays.stream(Month.values())
                        .map(
                                month ->
                                        new ClientAnnualStatementData.MonthlyIncome(
                                                month, totalsByMonth.get(month)))
                        .toList();

        BigDecimal totalYear =
                transactions.stream()
                        .map(Transaction::amount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        return receiptGeneratorPort.generateClientAnnualStatement(
                new ClientAnnualStatementData(issuer, client, year, monthlyIncomes, totalYear));
    }

    /**
     * Soma as despesas do usuário (em todas as contas) por categoria dentro do mês, ordenadas da
     * maior para a menor — só entram categorias com pelo menos uma despesa no período. Transações
     * sem categoria entram como "Sem categoria"; uma categoria excluída depois de usada entra como
     * "Categoria removida", já que o histórico da transação não pode sumir.
     */
    public byte[] generateCategoryExpenseReport(Long currentUserId, YearMonth referenceMonth) {
        User issuer = findUserOrThrow(currentUserId);

        LocalDate start = referenceMonth.atDay(1);
        LocalDate end = referenceMonth.plusMonths(1).atDay(1);

        List<Long> accountIds =
                accountRepositoryPort.findAllByUserId(currentUserId).stream()
                        .map(Account::id)
                        .toList();
        List<Transaction> expensesInPeriod =
                transactionRepositoryPort.findAllByAccountIds(accountIds).stream()
                        .filter(transaction -> transaction.type() == CategoryType.EXPENSE)
                        .filter(
                                transaction ->
                                        !transaction.transactionDate().isBefore(start)
                                                && transaction.transactionDate().isBefore(end))
                        .toList();

        Map<Long, String> categoryNameById =
                categoryRepositoryPort.findAllVisibleToUser(currentUserId).stream()
                        .collect(Collectors.toMap(Category::id, Category::name));

        Map<String, BigDecimal> totalsByCategoryName = new LinkedHashMap<>();
        for (Transaction transaction : expensesInPeriod) {
            String categoryName =
                    transaction.categoryId() == null
                            ? "Sem categoria"
                            : categoryNameById.getOrDefault(
                                    transaction.categoryId(), "Categoria removida");
            totalsByCategoryName.merge(categoryName, transaction.amount(), BigDecimal::add);
        }

        List<CategoryExpenseReportData.CategoryExpense> categoryExpenses =
                totalsByCategoryName.entrySet().stream()
                        .map(
                                entry ->
                                        new CategoryExpenseReportData.CategoryExpense(
                                                entry.getKey(), entry.getValue()))
                        .sorted(
                                Comparator.comparing(
                                                CategoryExpenseReportData.CategoryExpense::total)
                                        .reversed())
                        .toList();

        BigDecimal totalExpense =
                categoryExpenses.stream()
                        .map(CategoryExpenseReportData.CategoryExpense::total)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        return receiptGeneratorPort.generateCategoryExpenseReport(
                new CategoryExpenseReportData(
                        issuer, referenceMonth, categoryExpenses, totalExpense));
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
