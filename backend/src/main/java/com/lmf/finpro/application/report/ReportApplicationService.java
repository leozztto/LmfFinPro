package com.lmf.finpro.application.report;

import com.lmf.finpro.domain.exception.ResourceNotFoundException;
import com.lmf.finpro.domain.model.Account;
import com.lmf.finpro.domain.model.AccountStatementData;
import com.lmf.finpro.domain.model.Budget;
import com.lmf.finpro.domain.model.BudgetVsActualReportData;
import com.lmf.finpro.domain.model.Category;
import com.lmf.finpro.domain.model.CategoryExpenseReportData;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.Client;
import com.lmf.finpro.domain.model.ClientAnnualStatementData;
import com.lmf.finpro.domain.model.ClientReceiptData;
import com.lmf.finpro.domain.model.IncomeStatementData;
import com.lmf.finpro.domain.model.ReportFormat;
import com.lmf.finpro.domain.model.ReportGranularity;
import com.lmf.finpro.domain.model.Transaction;
import com.lmf.finpro.domain.model.TransactionExportData;
import com.lmf.finpro.domain.model.User;
import com.lmf.finpro.domain.port.out.AccountRepositoryPort;
import com.lmf.finpro.domain.port.out.BudgetRepositoryPort;
import com.lmf.finpro.domain.port.out.CategoryRepositoryPort;
import com.lmf.finpro.domain.port.out.ClientRepositoryPort;
import com.lmf.finpro.domain.port.out.ReceiptGeneratorPort;
import com.lmf.finpro.domain.port.out.ReportCsvExporterPort;
import com.lmf.finpro.domain.port.out.TransactionRepositoryPort;
import com.lmf.finpro.domain.port.out.UserRepositoryPort;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;
import java.time.Year;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReportApplicationService {

    private static final Locale PT_BR = Locale.of("pt", "BR");

    private final ClientRepositoryPort clientRepositoryPort;
    private final AccountRepositoryPort accountRepositoryPort;
    private final CategoryRepositoryPort categoryRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;
    private final TransactionRepositoryPort transactionRepositoryPort;
    private final BudgetRepositoryPort budgetRepositoryPort;
    private final ReceiptGeneratorPort receiptGeneratorPort;
    private final ReportCsvExporterPort reportCsvExporterPort;

    public byte[] generateClientReceipt(
            Long currentUserId, Long clientId, YearMonth referenceMonth, ReportFormat format) {
        ClientReceiptData data = buildClientReceiptData(currentUserId, clientId, referenceMonth);
        return format == ReportFormat.CSV
                ? reportCsvExporterPort.exportClientReceipt(data)
                : receiptGeneratorPort.generateClientReceipt(data);
    }

    private ClientReceiptData buildClientReceiptData(
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

        return new ClientReceiptData(issuer, client, referenceMonth, transactions, total);
    }

    /**
     * Saldo de abertura = saldo inicial da conta + tudo lançado antes do período; saldo de
     * fechamento = abertura + receitas - despesas do período. Reaproveita {@code
     * findAllByAccountIds} (já existente) e divide as transações em memória por data, em vez de
     * criar uma query nova só para isso.
     */
    public byte[] generateAccountStatement(
            Long currentUserId, Long accountId, YearMonth referenceMonth, ReportFormat format) {
        AccountStatementData data =
                buildAccountStatementData(currentUserId, accountId, referenceMonth);
        return format == ReportFormat.CSV
                ? reportCsvExporterPort.exportAccountStatement(data)
                : receiptGeneratorPort.generateAccountStatement(data);
    }

    private AccountStatementData buildAccountStatementData(
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

    /**
     * Soma as receitas do cliente em cada um dos 12 meses do ano (mesmo os que ficarem zerados),
     * reaproveitando {@code findAllByClientIdAndTypeAndDateBetween} com o intervalo do ano inteiro
     * em vez de um mês, igual ao recibo mensal.
     */
    public byte[] generateClientAnnualStatement(
            Long currentUserId, Long clientId, Year year, ReportFormat format) {
        ClientAnnualStatementData data =
                buildClientAnnualStatementData(currentUserId, clientId, year);
        return format == ReportFormat.CSV
                ? reportCsvExporterPort.exportClientAnnualStatement(data)
                : receiptGeneratorPort.generateClientAnnualStatement(data);
    }

    private ClientAnnualStatementData buildClientAnnualStatementData(
            Long currentUserId, Long clientId, Year year) {
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

        return new ClientAnnualStatementData(issuer, client, year, monthlyIncomes, totalYear);
    }

    /**
     * Soma as despesas do usuário (em todas as contas) por categoria dentro do mês, ordenadas da
     * maior para a menor — só entram categorias com pelo menos uma despesa no período. Transações
     * sem categoria entram como "Sem categoria"; uma categoria excluída depois de usada entra como
     * "Categoria removida", já que o histórico da transação não pode sumir.
     */
    public byte[] generateCategoryExpenseReport(
            Long currentUserId, YearMonth referenceMonth, ReportFormat format) {
        CategoryExpenseReportData data =
                buildCategoryExpenseReportData(currentUserId, referenceMonth);
        return format == ReportFormat.CSV
                ? reportCsvExporterPort.exportCategoryExpenseReport(data)
                : receiptGeneratorPort.generateCategoryExpenseReport(data);
    }

    private CategoryExpenseReportData buildCategoryExpenseReportData(
            Long currentUserId, YearMonth referenceMonth) {
        User issuer = findUserOrThrow(currentUserId);

        LocalDate start = referenceMonth.atDay(1);
        LocalDate end = referenceMonth.plusMonths(1).atDay(1);

        List<Long> accountIds =
                accountRepositoryPort.findAllByUserId(currentUserId).stream()
                        .map(Account::id)
                        .toList();
        List<Transaction> expensesInPeriod =
                transactionRepositoryPort.findAllByAccountIds(accountIds).stream()
                        .filter(transaction -> transaction.transferId() == null)
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

        return new CategoryExpenseReportData(
                issuer, referenceMonth, categoryExpenses, totalExpense);
    }

    /**
     * Receita, despesa e resultado consolidados por período (mês, trimestre ou o ano inteiro,
     * conforme {@code granularity}) dentro do ano informado, em todas as contas do usuário —
     * transferências entre contas próprias são excluídas, igual ao Dashboard.
     */
    public byte[] generateIncomeStatement(
            Long currentUserId, Year year, ReportGranularity granularity, ReportFormat format) {
        IncomeStatementData data = buildIncomeStatementData(currentUserId, year, granularity);
        return format == ReportFormat.CSV
                ? reportCsvExporterPort.exportIncomeStatement(data)
                : receiptGeneratorPort.generateIncomeStatement(data);
    }

    private IncomeStatementData buildIncomeStatementData(
            Long currentUserId, Year year, ReportGranularity granularity) {
        User issuer = findUserOrThrow(currentUserId);

        List<Long> accountIds =
                accountRepositoryPort.findAllByUserId(currentUserId).stream()
                        .map(Account::id)
                        .toList();
        List<Transaction> transactionsInYear =
                transactionRepositoryPort.findAllByAccountIds(accountIds).stream()
                        .filter(transaction -> transaction.transferId() == null)
                        .filter(
                                transaction ->
                                        transaction.transactionDate().getYear() == year.getValue())
                        .toList();

        List<IncomeStatementData.PeriodResult> periods =
                switch (granularity) {
                    case MONTHLY -> monthlyPeriods(transactionsInYear);
                    case QUARTERLY -> quarterlyPeriods(transactionsInYear);
                    case YEARLY -> yearlyPeriod(transactionsInYear, year);
                };

        BigDecimal totalIncome =
                periods.stream()
                        .map(IncomeStatementData.PeriodResult::income)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalExpense =
                periods.stream()
                        .map(IncomeStatementData.PeriodResult::expense)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalResult = totalIncome.subtract(totalExpense);

        return new IncomeStatementData(
                issuer, year, granularity, periods, totalIncome, totalExpense, totalResult);
    }

    private List<IncomeStatementData.PeriodResult> monthlyPeriods(List<Transaction> transactions) {
        Map<Month, BigDecimal> incomeByMonth = new EnumMap<>(Month.class);
        Map<Month, BigDecimal> expenseByMonth = new EnumMap<>(Month.class);
        for (Month month : Month.values()) {
            incomeByMonth.put(month, BigDecimal.ZERO);
            expenseByMonth.put(month, BigDecimal.ZERO);
        }
        for (Transaction transaction : transactions) {
            Month month = transaction.transactionDate().getMonth();
            if (transaction.type() == CategoryType.INCOME) {
                incomeByMonth.merge(month, transaction.amount(), BigDecimal::add);
            } else {
                expenseByMonth.merge(month, transaction.amount(), BigDecimal::add);
            }
        }
        return Arrays.stream(Month.values())
                .map(
                        month ->
                                periodResult(
                                        capitalizeMonth(month),
                                        incomeByMonth.get(month),
                                        expenseByMonth.get(month)))
                .toList();
    }

    private List<IncomeStatementData.PeriodResult> quarterlyPeriods(
            List<Transaction> transactions) {
        List<IncomeStatementData.PeriodResult> periods = new ArrayList<>();
        for (int quarter = 1; quarter <= 4; quarter++) {
            int startMonth = (quarter - 1) * 3 + 1;
            int endMonthExclusive = startMonth + 3;
            BigDecimal income = BigDecimal.ZERO;
            BigDecimal expense = BigDecimal.ZERO;
            for (Transaction transaction : transactions) {
                int monthValue = transaction.transactionDate().getMonthValue();
                if (monthValue < startMonth || monthValue >= endMonthExclusive) {
                    continue;
                }
                if (transaction.type() == CategoryType.INCOME) {
                    income = income.add(transaction.amount());
                } else {
                    expense = expense.add(transaction.amount());
                }
            }
            periods.add(periodResult(quarter + "º trimestre", income, expense));
        }
        return periods;
    }

    private List<IncomeStatementData.PeriodResult> yearlyPeriod(
            List<Transaction> transactions, Year year) {
        BigDecimal income =
                transactions.stream()
                        .filter(transaction -> transaction.type() == CategoryType.INCOME)
                        .map(Transaction::amount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal expense =
                transactions.stream()
                        .filter(transaction -> transaction.type() == CategoryType.EXPENSE)
                        .map(Transaction::amount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        return List.of(periodResult(year.toString(), income, expense));
    }

    private IncomeStatementData.PeriodResult periodResult(
            String label, BigDecimal income, BigDecimal expense) {
        return new IncomeStatementData.PeriodResult(
                label, income, expense, income.subtract(expense));
    }

    private String capitalizeMonth(Month month) {
        String name = month.getDisplayName(TextStyle.FULL, PT_BR);
        return name.substring(0, 1).toUpperCase(PT_BR) + name.substring(1);
    }

    /**
     * Compara, para cada orçamento cadastrado no mês, o limite definido com o total já gasto na
     * categoria (mesmo cálculo de {@code BudgetApplicationService.calculateSpent}, refeito aqui
     * para não acoplar um application service a outro), ordenado do maior percentual de uso para o
     * menor — assim os orçamentos estourados ou perto do limite aparecem primeiro.
     */
    public byte[] generateBudgetVsActualReport(
            Long currentUserId, YearMonth referenceMonth, ReportFormat format) {
        BudgetVsActualReportData data =
                buildBudgetVsActualReportData(currentUserId, referenceMonth);
        return format == ReportFormat.CSV
                ? reportCsvExporterPort.exportBudgetVsActualReport(data)
                : receiptGeneratorPort.generateBudgetVsActualReport(data);
    }

    private BudgetVsActualReportData buildBudgetVsActualReportData(
            Long currentUserId, YearMonth referenceMonth) {
        User issuer = findUserOrThrow(currentUserId);

        List<Budget> budgets =
                budgetRepositoryPort.findAllByUserId(currentUserId).stream()
                        .filter(budget -> budget.referenceMonth().equals(referenceMonth))
                        .toList();

        Map<Long, String> categoryNameById =
                categoryRepositoryPort.findAllVisibleToUser(currentUserId).stream()
                        .collect(Collectors.toMap(Category::id, Category::name));

        List<BudgetVsActualReportData.BudgetComparison> comparisons =
                budgets.stream()
                        .map(budget -> budgetComparison(budget, categoryNameById))
                        .sorted(Comparator.comparing(this::usageRatio).reversed())
                        .toList();

        BigDecimal totalLimit =
                comparisons.stream()
                        .map(BudgetVsActualReportData.BudgetComparison::limitValue)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalSpent =
                comparisons.stream()
                        .map(BudgetVsActualReportData.BudgetComparison::spentValue)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new BudgetVsActualReportData(
                issuer, referenceMonth, comparisons, totalLimit, totalSpent);
    }

    private BudgetVsActualReportData.BudgetComparison budgetComparison(
            Budget budget, Map<Long, String> categoryNameById) {
        BigDecimal spent =
                transactionRepositoryPort.sumAmountByUserIdAndCategoryIdAndTypeBetween(
                        budget.userId(),
                        budget.categoryId(),
                        CategoryType.EXPENSE,
                        budget.referenceMonth().atDay(1),
                        budget.referenceMonth().plusMonths(1).atDay(1));
        String categoryName =
                categoryNameById.getOrDefault(budget.categoryId(), "Categoria removida");
        BigDecimal difference = budget.limitValue().subtract(spent);
        return new BudgetVsActualReportData.BudgetComparison(
                categoryName,
                budget.limitValue(),
                spent,
                difference,
                spent.compareTo(budget.limitValue()) > 0);
    }

    private BigDecimal usageRatio(BudgetVsActualReportData.BudgetComparison comparison) {
        if (comparison.limitValue().compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return comparison.spentValue().divide(comparison.limitValue(), 4, RoundingMode.HALF_UP);
    }

    /**
     * Exporta, em CSV, o extrato bruto de transações do usuário (em todas as contas) dentro do mês,
     * ordenado por data — ao contrário dos relatórios agregados, inclui transferências, já que o
     * objetivo aqui é auditar o extrato completo, não somar receita/despesa.
     */
    public byte[] generateTransactionExport(
            Long currentUserId, YearMonth referenceMonth, ReportFormat format) {
        TransactionExportData data = buildTransactionExportData(currentUserId, referenceMonth);
        return format == ReportFormat.PDF
                ? receiptGeneratorPort.generateTransactionExport(data)
                : reportCsvExporterPort.exportTransactions(data);
    }

    private TransactionExportData buildTransactionExportData(
            Long currentUserId, YearMonth referenceMonth) {
        LocalDate start = referenceMonth.atDay(1);
        LocalDate end = referenceMonth.plusMonths(1).atDay(1);

        List<Account> accounts = accountRepositoryPort.findAllByUserId(currentUserId);
        Map<Long, String> accountNameById =
                accounts.stream().collect(Collectors.toMap(Account::id, Account::name));
        List<Long> accountIds = accounts.stream().map(Account::id).toList();

        Map<Long, String> categoryNameById =
                categoryRepositoryPort.findAllVisibleToUser(currentUserId).stream()
                        .collect(Collectors.toMap(Category::id, Category::name));
        Map<Long, String> clientNameById =
                clientRepositoryPort.findAllByUserId(currentUserId).stream()
                        .collect(Collectors.toMap(Client::id, Client::name));

        List<TransactionExportData.TransactionExportRow> rows =
                transactionRepositoryPort.findAllByAccountIds(accountIds).stream()
                        .filter(
                                transaction ->
                                        !transaction.transactionDate().isBefore(start)
                                                && transaction.transactionDate().isBefore(end))
                        .sorted(Comparator.comparing(Transaction::transactionDate))
                        .map(
                                transaction ->
                                        new TransactionExportData.TransactionExportRow(
                                                transaction.transactionDate(),
                                                accountNameById.getOrDefault(
                                                        transaction.accountId(), "Conta removida"),
                                                transaction.categoryId() == null
                                                        ? "Sem categoria"
                                                        : categoryNameById.getOrDefault(
                                                                transaction.categoryId(),
                                                                "Categoria removida"),
                                                transaction.clientId() == null
                                                        ? ""
                                                        : clientNameById.getOrDefault(
                                                                transaction.clientId(),
                                                                "Cliente removido"),
                                                transaction.description(),
                                                transaction.type(),
                                                transaction.status(),
                                                transaction.amount()))
                        .toList();

        return new TransactionExportData(referenceMonth, rows);
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
