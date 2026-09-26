package com.lmf.finpro.application.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lmf.finpro.domain.exception.ResourceNotFoundException;
import com.lmf.finpro.domain.model.Account;
import com.lmf.finpro.domain.model.AccountStatementData;
import com.lmf.finpro.domain.model.AccountType;
import com.lmf.finpro.domain.model.Address;
import com.lmf.finpro.domain.model.BrazilianState;
import com.lmf.finpro.domain.model.Budget;
import com.lmf.finpro.domain.model.BudgetVsActualReportData;
import com.lmf.finpro.domain.model.Category;
import com.lmf.finpro.domain.model.CategoryExpenseReportData;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.Client;
import com.lmf.finpro.domain.model.ClientAnnualStatementData;
import com.lmf.finpro.domain.model.ClientReceiptData;
import com.lmf.finpro.domain.model.ClientWorkType;
import com.lmf.finpro.domain.model.DocumentType;
import com.lmf.finpro.domain.model.IncomeStatementData;
import com.lmf.finpro.domain.model.ReportFormat;
import com.lmf.finpro.domain.model.ReportGranularity;
import com.lmf.finpro.domain.model.TaxRegime;
import com.lmf.finpro.domain.model.Transaction;
import com.lmf.finpro.domain.model.TransactionExportData;
import com.lmf.finpro.domain.model.TransactionOrigin;
import com.lmf.finpro.domain.model.TransactionStatus;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.Year;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReportApplicationServiceTest {

    @Mock private ClientRepositoryPort clientRepositoryPort;
    @Mock private AccountRepositoryPort accountRepositoryPort;
    @Mock private CategoryRepositoryPort categoryRepositoryPort;
    @Mock private UserRepositoryPort userRepositoryPort;
    @Mock private TransactionRepositoryPort transactionRepositoryPort;
    @Mock private BudgetRepositoryPort budgetRepositoryPort;
    @Mock private ReceiptGeneratorPort receiptGeneratorPort;
    @Mock private ReportCsvExporterPort reportCsvExporterPort;

    @InjectMocks private ReportApplicationService service;

    private static Client ownedClient() {
        return new Client(
                1L,
                10L,
                "Cliente X",
                "cliente@x.com",
                "11987654321",
                DocumentType.CPF,
                "52998224725",
                ClientWorkType.PJ,
                null,
                null,
                true);
    }

    private static Account ownedAccount() {
        return new Account(
                5L,
                10L,
                "Conta Corrente",
                AccountType.CHECKING,
                BigDecimal.valueOf(1000),
                LocalDateTime.now());
    }

    private static User issuer() {
        Address address =
                new Address(
                        "01310100",
                        "Av. Paulista",
                        "1000",
                        null,
                        "Bela Vista",
                        "São Paulo",
                        BrazilianState.SP);
        return new User(
                10L,
                "Prestador",
                "prestador@x.com",
                "hash",
                DocumentType.CPF,
                "11144477735",
                "11999998888",
                TaxRegime.AUTONOMO,
                address,
                LocalDateTime.now(),
                0);
    }

    @Test
    void throwsWhenClientDoesNotBelongToCurrentUser() {
        when(clientRepositoryPort.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                service.generateClientReceipt(
                                        10L, 1L, YearMonth.of(2026, 9), ReportFormat.PDF))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void throwsWhenClientBelongsToAnotherUser() {
        Client otherUsersClient =
                new Client(
                        1L,
                        999L,
                        "Outro",
                        null,
                        null,
                        DocumentType.CPF,
                        "52998224725",
                        ClientWorkType.PJ,
                        null,
                        null,
                        true);
        when(clientRepositoryPort.findById(1L)).thenReturn(Optional.of(otherUsersClient));

        assertThatThrownBy(
                        () ->
                                service.generateClientReceipt(
                                        10L, 1L, YearMonth.of(2026, 9), ReportFormat.PDF))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void sumsTransactionsAndPassesEverythingToTheGenerator() {
        User issuer = issuer();
        when(clientRepositoryPort.findById(1L)).thenReturn(Optional.of(ownedClient()));
        when(userRepositoryPort.findById(10L)).thenReturn(Optional.of(issuer));
        Transaction t1 =
                new Transaction(
                        1L,
                        5L,
                        null,
                        1L,
                        "Serviço A",
                        BigDecimal.valueOf(1000),
                        LocalDate.of(2026, 9, 5),
                        CategoryType.INCOME,
                        TransactionOrigin.MANUAL,
                        LocalDateTime.now(),
                        null,
                        null);
        Transaction t2 =
                new Transaction(
                        2L,
                        5L,
                        null,
                        1L,
                        "Serviço B",
                        BigDecimal.valueOf(500),
                        LocalDate.of(2026, 9, 20),
                        CategoryType.INCOME,
                        TransactionOrigin.MANUAL,
                        LocalDateTime.now(),
                        null,
                        null);
        when(transactionRepositoryPort.findAllByClientIdAndTypeAndDateBetween(
                        eq(1L),
                        eq(CategoryType.INCOME),
                        eq(LocalDate.of(2026, 9, 1)),
                        eq(LocalDate.of(2026, 10, 1))))
                .thenReturn(List.of(t1, t2));
        when(receiptGeneratorPort.generateClientReceipt(any())).thenReturn(new byte[] {1, 2, 3});

        byte[] result =
                service.generateClientReceipt(10L, 1L, YearMonth.of(2026, 9), ReportFormat.PDF);

        assertThat(result).containsExactly(1, 2, 3);
        ArgumentCaptor<ClientReceiptData> captor = ArgumentCaptor.forClass(ClientReceiptData.class);
        verify(receiptGeneratorPort).generateClientReceipt(captor.capture());
        ClientReceiptData data = captor.getValue();
        assertThat(data.total()).isEqualByComparingTo("1500");
        assertThat(data.transactions()).containsExactly(t1, t2);
        assertThat(data.client()).isEqualTo(ownedClient());
        assertThat(data.issuer()).isEqualTo(issuer);
        assertThat(data.referenceMonth()).isEqualTo(YearMonth.of(2026, 9));
    }

    @Test
    void totalIsZeroWhenThereAreNoTransactionsInThePeriod() {
        when(clientRepositoryPort.findById(1L)).thenReturn(Optional.of(ownedClient()));
        when(userRepositoryPort.findById(10L)).thenReturn(Optional.of(issuer()));
        when(transactionRepositoryPort.findAllByClientIdAndTypeAndDateBetween(
                        any(), any(), any(), any()))
                .thenReturn(List.of());
        when(receiptGeneratorPort.generateClientReceipt(any())).thenReturn(new byte[0]);

        service.generateClientReceipt(10L, 1L, YearMonth.of(2026, 9), ReportFormat.PDF);

        ArgumentCaptor<ClientReceiptData> captor = ArgumentCaptor.forClass(ClientReceiptData.class);
        verify(receiptGeneratorPort).generateClientReceipt(captor.capture());
        assertThat(captor.getValue().total()).isEqualByComparingTo("0");
    }

    @Test
    void generateAccountStatementThrowsWhenAccountDoesNotBelongToCurrentUser() {
        when(accountRepositoryPort.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                service.generateAccountStatement(
                                        10L, 5L, YearMonth.of(2026, 9), ReportFormat.PDF))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void generateAccountStatementThrowsWhenAccountBelongsToAnotherUser() {
        Account otherUsersAccount =
                new Account(
                        5L,
                        999L,
                        "Conta",
                        AccountType.CHECKING,
                        BigDecimal.ZERO,
                        LocalDateTime.now());
        when(accountRepositoryPort.findById(5L)).thenReturn(Optional.of(otherUsersAccount));

        assertThatThrownBy(
                        () ->
                                service.generateAccountStatement(
                                        10L, 5L, YearMonth.of(2026, 9), ReportFormat.PDF))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void generateAccountStatementSeparatesTransactionsBeforeAndDuringThePeriod() {
        Account account = ownedAccount();
        User issuer = issuer();
        when(accountRepositoryPort.findById(5L)).thenReturn(Optional.of(account));
        when(userRepositoryPort.findById(10L)).thenReturn(Optional.of(issuer));

        Transaction beforePeriod =
                new Transaction(
                        1L,
                        5L,
                        null,
                        null,
                        "Antes do período",
                        BigDecimal.valueOf(200),
                        LocalDate.of(2026, 8, 15),
                        CategoryType.INCOME,
                        TransactionOrigin.MANUAL,
                        LocalDateTime.now(),
                        null,
                        null);
        Transaction incomeInPeriod =
                new Transaction(
                        2L,
                        5L,
                        null,
                        null,
                        "Receita do mês",
                        BigDecimal.valueOf(1000),
                        LocalDate.of(2026, 9, 5),
                        CategoryType.INCOME,
                        TransactionOrigin.MANUAL,
                        LocalDateTime.now(),
                        null,
                        null);
        Transaction expenseInPeriod =
                new Transaction(
                        3L,
                        5L,
                        null,
                        null,
                        "Despesa do mês",
                        BigDecimal.valueOf(300),
                        LocalDate.of(2026, 9, 20),
                        CategoryType.EXPENSE,
                        TransactionOrigin.MANUAL,
                        LocalDateTime.now(),
                        null,
                        null);
        when(transactionRepositoryPort.findAllByAccountIds(List.of(5L)))
                .thenReturn(List.of(expenseInPeriod, beforePeriod, incomeInPeriod));
        when(receiptGeneratorPort.generateAccountStatement(any())).thenReturn(new byte[] {9});

        byte[] result =
                service.generateAccountStatement(10L, 5L, YearMonth.of(2026, 9), ReportFormat.PDF);

        assertThat(result).containsExactly(9);
        ArgumentCaptor<AccountStatementData> captor =
                ArgumentCaptor.forClass(AccountStatementData.class);
        verify(receiptGeneratorPort).generateAccountStatement(captor.capture());
        AccountStatementData data = captor.getValue();
        // saldo inicial (1000) + transação de agosto (200) = 1200
        assertThat(data.openingBalance()).isEqualByComparingTo("1200");
        assertThat(data.transactions()).containsExactly(incomeInPeriod, expenseInPeriod);
        assertThat(data.totalIncome()).isEqualByComparingTo("1000");
        assertThat(data.totalExpense()).isEqualByComparingTo("300");
        // 1200 + 1000 - 300 = 1900
        assertThat(data.closingBalance()).isEqualByComparingTo("1900");
        assertThat(data.account()).isEqualTo(account);
        assertThat(data.issuer()).isEqualTo(issuer);
        assertThat(data.referenceMonth()).isEqualTo(YearMonth.of(2026, 9));
    }

    @Test
    void generateAccountStatementUsesInitialBalanceAsOpeningWhenThereAreNoEarlierTransactions() {
        when(accountRepositoryPort.findById(5L)).thenReturn(Optional.of(ownedAccount()));
        when(userRepositoryPort.findById(10L)).thenReturn(Optional.of(issuer()));
        when(transactionRepositoryPort.findAllByAccountIds(List.of(5L))).thenReturn(List.of());
        when(receiptGeneratorPort.generateAccountStatement(any())).thenReturn(new byte[0]);

        service.generateAccountStatement(10L, 5L, YearMonth.of(2026, 9), ReportFormat.PDF);

        ArgumentCaptor<AccountStatementData> captor =
                ArgumentCaptor.forClass(AccountStatementData.class);
        verify(receiptGeneratorPort).generateAccountStatement(captor.capture());
        AccountStatementData data = captor.getValue();
        assertThat(data.openingBalance()).isEqualByComparingTo("1000");
        assertThat(data.closingBalance()).isEqualByComparingTo("1000");
        assertThat(data.transactions()).isEmpty();
    }

    @Test
    void generateClientAnnualStatementThrowsWhenClientDoesNotBelongToCurrentUser() {
        when(clientRepositoryPort.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                service.generateClientAnnualStatement(
                                        10L, 1L, Year.of(2026), ReportFormat.PDF))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void generateClientAnnualStatementFillsAllTwelveMonthsAndSumsOnlyTheGivenYear() {
        Client client = ownedClient();
        User issuer = issuer();
        when(clientRepositoryPort.findById(1L)).thenReturn(Optional.of(client));
        when(userRepositoryPort.findById(10L)).thenReturn(Optional.of(issuer));

        Transaction january =
                new Transaction(
                        1L,
                        5L,
                        null,
                        1L,
                        "Serviço de janeiro",
                        BigDecimal.valueOf(1000),
                        LocalDate.of(2026, 1, 10),
                        CategoryType.INCOME,
                        TransactionOrigin.MANUAL,
                        LocalDateTime.now(),
                        null,
                        null);
        Transaction anotherInJanuary =
                new Transaction(
                        2L,
                        5L,
                        null,
                        1L,
                        "Segundo serviço de janeiro",
                        BigDecimal.valueOf(500),
                        LocalDate.of(2026, 1, 20),
                        CategoryType.INCOME,
                        TransactionOrigin.MANUAL,
                        LocalDateTime.now(),
                        null,
                        null);
        Transaction december =
                new Transaction(
                        3L,
                        5L,
                        null,
                        1L,
                        "Serviço de dezembro",
                        BigDecimal.valueOf(300),
                        LocalDate.of(2026, 12, 1),
                        CategoryType.INCOME,
                        TransactionOrigin.MANUAL,
                        LocalDateTime.now(),
                        null,
                        null);
        when(transactionRepositoryPort.findAllByClientIdAndTypeAndDateBetween(
                        eq(1L),
                        eq(CategoryType.INCOME),
                        eq(LocalDate.of(2026, 1, 1)),
                        eq(LocalDate.of(2027, 1, 1))))
                .thenReturn(List.of(january, anotherInJanuary, december));
        when(receiptGeneratorPort.generateClientAnnualStatement(any())).thenReturn(new byte[] {7});

        byte[] result =
                service.generateClientAnnualStatement(10L, 1L, Year.of(2026), ReportFormat.PDF);

        assertThat(result).containsExactly(7);
        ArgumentCaptor<ClientAnnualStatementData> captor =
                ArgumentCaptor.forClass(ClientAnnualStatementData.class);
        verify(receiptGeneratorPort).generateClientAnnualStatement(captor.capture());
        ClientAnnualStatementData data = captor.getValue();

        assertThat(data.monthlyIncomes()).hasSize(12);
        assertThat(data.monthlyIncomes())
                .extracting(ClientAnnualStatementData.MonthlyIncome::month)
                .containsExactly(Month.values());
        assertThat(monthlyTotal(data, Month.JANUARY)).isEqualByComparingTo("1500");
        assertThat(monthlyTotal(data, Month.DECEMBER)).isEqualByComparingTo("300");
        assertThat(monthlyTotal(data, Month.FEBRUARY)).isEqualByComparingTo("0");
        assertThat(data.totalYear()).isEqualByComparingTo("1800");
        assertThat(data.client()).isEqualTo(client);
        assertThat(data.issuer()).isEqualTo(issuer);
        assertThat(data.referenceYear()).isEqualTo(Year.of(2026));
    }

    @Test
    void generateClientAnnualStatementReturnsAllZeroMonthsWhenThereAreNoTransactions() {
        when(clientRepositoryPort.findById(1L)).thenReturn(Optional.of(ownedClient()));
        when(userRepositoryPort.findById(10L)).thenReturn(Optional.of(issuer()));
        when(transactionRepositoryPort.findAllByClientIdAndTypeAndDateBetween(
                        any(), any(), any(), any()))
                .thenReturn(List.of());
        when(receiptGeneratorPort.generateClientAnnualStatement(any())).thenReturn(new byte[0]);

        service.generateClientAnnualStatement(10L, 1L, Year.of(2026), ReportFormat.PDF);

        ArgumentCaptor<ClientAnnualStatementData> captor =
                ArgumentCaptor.forClass(ClientAnnualStatementData.class);
        verify(receiptGeneratorPort).generateClientAnnualStatement(captor.capture());
        ClientAnnualStatementData data = captor.getValue();
        assertThat(data.totalYear()).isEqualByComparingTo("0");
        assertThat(data.monthlyIncomes())
                .allMatch(monthly -> monthly.total().compareTo(BigDecimal.ZERO) == 0);
    }

    @Test
    void generateCategoryExpenseReportGroupsExpensesAcrossAllAccountsSortedDescending() {
        User issuer = issuer();
        when(userRepositoryPort.findById(10L)).thenReturn(Optional.of(issuer));

        Account checking =
                new Account(
                        5L,
                        10L,
                        "Conta Corrente",
                        AccountType.CHECKING,
                        BigDecimal.ZERO,
                        LocalDateTime.now());
        Account wallet =
                new Account(
                        6L,
                        10L,
                        "Carteira",
                        AccountType.WALLET,
                        BigDecimal.ZERO,
                        LocalDateTime.now());
        when(accountRepositoryPort.findAllByUserId(10L)).thenReturn(List.of(checking, wallet));

        Category rent = new Category(1L, 10L, "Aluguel", CategoryType.EXPENSE, null, null);
        Category software = new Category(2L, 10L, "Software", CategoryType.EXPENSE, null, null);
        when(categoryRepositoryPort.findAllVisibleToUser(10L)).thenReturn(List.of(rent, software));

        Transaction rentExpense =
                new Transaction(
                        1L,
                        5L,
                        1L,
                        null,
                        "Aluguel escritório",
                        BigDecimal.valueOf(1500),
                        LocalDate.of(2026, 9, 5),
                        CategoryType.EXPENSE,
                        TransactionOrigin.MANUAL,
                        LocalDateTime.now(),
                        null,
                        null);
        Transaction softwareExpense =
                new Transaction(
                        2L,
                        6L,
                        2L,
                        null,
                        "Assinatura",
                        BigDecimal.valueOf(100),
                        LocalDate.of(2026, 9, 10),
                        CategoryType.EXPENSE,
                        TransactionOrigin.MANUAL,
                        LocalDateTime.now(),
                        null,
                        null);
        Transaction uncategorizedExpense =
                new Transaction(
                        3L,
                        5L,
                        null,
                        null,
                        "Despesa sem categoria",
                        BigDecimal.valueOf(50),
                        LocalDate.of(2026, 9, 12),
                        CategoryType.EXPENSE,
                        TransactionOrigin.MANUAL,
                        LocalDateTime.now(),
                        null,
                        null);
        Transaction income =
                new Transaction(
                        4L,
                        5L,
                        null,
                        null,
                        "Receita não deve entrar",
                        BigDecimal.valueOf(9999),
                        LocalDate.of(2026, 9, 15),
                        CategoryType.INCOME,
                        TransactionOrigin.MANUAL,
                        LocalDateTime.now(),
                        null,
                        null);
        Transaction outsidePeriod =
                new Transaction(
                        5L,
                        5L,
                        1L,
                        null,
                        "Aluguel de agosto",
                        BigDecimal.valueOf(1500),
                        LocalDate.of(2026, 8, 5),
                        CategoryType.EXPENSE,
                        TransactionOrigin.MANUAL,
                        LocalDateTime.now(),
                        null,
                        null);
        when(transactionRepositoryPort.findAllByAccountIds(List.of(5L, 6L)))
                .thenReturn(
                        List.of(
                                rentExpense,
                                softwareExpense,
                                uncategorizedExpense,
                                income,
                                outsidePeriod));
        when(receiptGeneratorPort.generateCategoryExpenseReport(any())).thenReturn(new byte[] {4});

        byte[] result =
                service.generateCategoryExpenseReport(10L, YearMonth.of(2026, 9), ReportFormat.PDF);

        assertThat(result).containsExactly(4);
        ArgumentCaptor<CategoryExpenseReportData> captor =
                ArgumentCaptor.forClass(CategoryExpenseReportData.class);
        verify(receiptGeneratorPort).generateCategoryExpenseReport(captor.capture());
        CategoryExpenseReportData data = captor.getValue();

        assertThat(data.categoryExpenses())
                .extracting(CategoryExpenseReportData.CategoryExpense::categoryName)
                .containsExactly("Aluguel", "Software", "Sem categoria");
        assertThat(data.categoryExpenses().get(0).total()).isEqualByComparingTo("1500");
        assertThat(data.categoryExpenses().get(1).total()).isEqualByComparingTo("100");
        assertThat(data.categoryExpenses().get(2).total()).isEqualByComparingTo("50");
        assertThat(data.totalExpense()).isEqualByComparingTo("1650");
        assertThat(data.issuer()).isEqualTo(issuer);
        assertThat(data.referenceMonth()).isEqualTo(YearMonth.of(2026, 9));
    }

    @Test
    void generateCategoryExpenseReportUsesRemovedCategoryLabelWhenCategoryNoLongerExists() {
        when(userRepositoryPort.findById(10L)).thenReturn(Optional.of(issuer()));
        Account account = ownedAccount();
        when(accountRepositoryPort.findAllByUserId(10L)).thenReturn(List.of(account));
        when(categoryRepositoryPort.findAllVisibleToUser(10L)).thenReturn(List.of());

        Transaction expenseWithDeletedCategory =
                new Transaction(
                        1L,
                        5L,
                        999L,
                        null,
                        "Despesa antiga",
                        BigDecimal.valueOf(200),
                        LocalDate.of(2026, 9, 5),
                        CategoryType.EXPENSE,
                        TransactionOrigin.MANUAL,
                        LocalDateTime.now(),
                        null,
                        null);
        when(transactionRepositoryPort.findAllByAccountIds(List.of(5L)))
                .thenReturn(List.of(expenseWithDeletedCategory));
        when(receiptGeneratorPort.generateCategoryExpenseReport(any())).thenReturn(new byte[0]);

        service.generateCategoryExpenseReport(10L, YearMonth.of(2026, 9), ReportFormat.PDF);

        ArgumentCaptor<CategoryExpenseReportData> captor =
                ArgumentCaptor.forClass(CategoryExpenseReportData.class);
        verify(receiptGeneratorPort).generateCategoryExpenseReport(captor.capture());
        assertThat(captor.getValue().categoryExpenses())
                .extracting(CategoryExpenseReportData.CategoryExpense::categoryName)
                .containsExactly("Categoria removida");
    }

    @Test
    void generateCategoryExpenseReportReturnsEmptyListWhenThereAreNoExpenses() {
        when(userRepositoryPort.findById(10L)).thenReturn(Optional.of(issuer()));
        when(accountRepositoryPort.findAllByUserId(10L)).thenReturn(List.of(ownedAccount()));
        when(categoryRepositoryPort.findAllVisibleToUser(10L)).thenReturn(List.of());
        when(transactionRepositoryPort.findAllByAccountIds(List.of(5L))).thenReturn(List.of());
        when(receiptGeneratorPort.generateCategoryExpenseReport(any())).thenReturn(new byte[0]);

        service.generateCategoryExpenseReport(10L, YearMonth.of(2026, 9), ReportFormat.PDF);

        ArgumentCaptor<CategoryExpenseReportData> captor =
                ArgumentCaptor.forClass(CategoryExpenseReportData.class);
        verify(receiptGeneratorPort).generateCategoryExpenseReport(captor.capture());
        CategoryExpenseReportData data = captor.getValue();
        assertThat(data.categoryExpenses()).isEmpty();
        assertThat(data.totalExpense()).isEqualByComparingTo("0");
    }

    @Test
    void generateIncomeStatementMonthlyAggregatesPerMonthAndExcludesTransfersAndOtherYears() {
        User issuer = issuer();
        when(userRepositoryPort.findById(10L)).thenReturn(Optional.of(issuer));
        Account account = ownedAccount();
        when(accountRepositoryPort.findAllByUserId(10L)).thenReturn(List.of(account));

        Transaction januaryIncome =
                new Transaction(
                        1L,
                        5L,
                        null,
                        null,
                        "Receita de janeiro",
                        BigDecimal.valueOf(1000),
                        LocalDate.of(2026, 1, 10),
                        CategoryType.INCOME,
                        TransactionOrigin.MANUAL,
                        LocalDateTime.now(),
                        null,
                        null);
        Transaction januaryExpense =
                new Transaction(
                        2L,
                        5L,
                        null,
                        null,
                        "Despesa de janeiro",
                        BigDecimal.valueOf(300),
                        LocalDate.of(2026, 1, 15),
                        CategoryType.EXPENSE,
                        TransactionOrigin.MANUAL,
                        LocalDateTime.now(),
                        null,
                        null);
        Transaction transfer =
                new Transaction(
                        3L,
                        5L,
                        null,
                        null,
                        "Transferência entre contas",
                        BigDecimal.valueOf(5000),
                        LocalDate.of(2026, 1, 20),
                        CategoryType.EXPENSE,
                        TransactionOrigin.MANUAL,
                        LocalDateTime.now(),
                        99L,
                        null);
        Transaction previousYear =
                new Transaction(
                        4L,
                        5L,
                        null,
                        null,
                        "Receita de dezembro do ano anterior",
                        BigDecimal.valueOf(9999),
                        LocalDate.of(2025, 12, 31),
                        CategoryType.INCOME,
                        TransactionOrigin.MANUAL,
                        LocalDateTime.now(),
                        null,
                        null);
        when(transactionRepositoryPort.findAllByAccountIds(List.of(5L)))
                .thenReturn(List.of(januaryIncome, januaryExpense, transfer, previousYear));
        when(receiptGeneratorPort.generateIncomeStatement(any())).thenReturn(new byte[] {1});

        byte[] result =
                service.generateIncomeStatement(
                        10L, Year.of(2026), ReportGranularity.MONTHLY, ReportFormat.PDF);

        assertThat(result).containsExactly(1);
        ArgumentCaptor<IncomeStatementData> captor =
                ArgumentCaptor.forClass(IncomeStatementData.class);
        verify(receiptGeneratorPort).generateIncomeStatement(captor.capture());
        IncomeStatementData data = captor.getValue();

        assertThat(data.periods()).hasSize(12);
        IncomeStatementData.PeriodResult january = data.periods().get(0);
        assertThat(january.income()).isEqualByComparingTo("1000");
        assertThat(january.expense()).isEqualByComparingTo("300");
        assertThat(january.result()).isEqualByComparingTo("700");
        assertThat(data.periods().get(1).income()).isEqualByComparingTo("0");
        assertThat(data.periods().get(1).expense()).isEqualByComparingTo("0");
        assertThat(data.totalIncome()).isEqualByComparingTo("1000");
        assertThat(data.totalExpense()).isEqualByComparingTo("300");
        assertThat(data.totalResult()).isEqualByComparingTo("700");
        assertThat(data.issuer()).isEqualTo(issuer);
        assertThat(data.referenceYear()).isEqualTo(Year.of(2026));
        assertThat(data.granularity()).isEqualTo(ReportGranularity.MONTHLY);
    }

    @Test
    void generateIncomeStatementQuarterlyAggregatesPerQuarter() {
        when(userRepositoryPort.findById(10L)).thenReturn(Optional.of(issuer()));
        Account account = ownedAccount();
        when(accountRepositoryPort.findAllByUserId(10L)).thenReturn(List.of(account));

        Transaction firstQuarterIncome =
                new Transaction(
                        1L,
                        5L,
                        null,
                        null,
                        "Receita de fevereiro",
                        BigDecimal.valueOf(1200),
                        LocalDate.of(2026, 2, 10),
                        CategoryType.INCOME,
                        TransactionOrigin.MANUAL,
                        LocalDateTime.now(),
                        null,
                        null);
        Transaction secondQuarterExpense =
                new Transaction(
                        2L,
                        5L,
                        null,
                        null,
                        "Despesa de maio",
                        BigDecimal.valueOf(400),
                        LocalDate.of(2026, 5, 5),
                        CategoryType.EXPENSE,
                        TransactionOrigin.MANUAL,
                        LocalDateTime.now(),
                        null,
                        null);
        when(transactionRepositoryPort.findAllByAccountIds(List.of(5L)))
                .thenReturn(List.of(firstQuarterIncome, secondQuarterExpense));
        when(receiptGeneratorPort.generateIncomeStatement(any())).thenReturn(new byte[0]);

        service.generateIncomeStatement(
                10L, Year.of(2026), ReportGranularity.QUARTERLY, ReportFormat.PDF);

        ArgumentCaptor<IncomeStatementData> captor =
                ArgumentCaptor.forClass(IncomeStatementData.class);
        verify(receiptGeneratorPort).generateIncomeStatement(captor.capture());
        IncomeStatementData data = captor.getValue();

        assertThat(data.periods()).hasSize(4);
        assertThat(data.periods().get(0).income()).isEqualByComparingTo("1200");
        assertThat(data.periods().get(0).expense()).isEqualByComparingTo("0");
        assertThat(data.periods().get(1).income()).isEqualByComparingTo("0");
        assertThat(data.periods().get(1).expense()).isEqualByComparingTo("400");
        assertThat(data.periods().get(2).result()).isEqualByComparingTo("0");
        assertThat(data.totalIncome()).isEqualByComparingTo("1200");
        assertThat(data.totalExpense()).isEqualByComparingTo("400");
    }

    @Test
    void generateIncomeStatementYearlySumsTheWholeYearIntoASinglePeriod() {
        when(userRepositoryPort.findById(10L)).thenReturn(Optional.of(issuer()));
        when(accountRepositoryPort.findAllByUserId(10L)).thenReturn(List.of(ownedAccount()));

        Transaction income =
                new Transaction(
                        1L,
                        5L,
                        null,
                        null,
                        "Receita",
                        BigDecimal.valueOf(2000),
                        LocalDate.of(2026, 3, 1),
                        CategoryType.INCOME,
                        TransactionOrigin.MANUAL,
                        LocalDateTime.now(),
                        null,
                        null);
        Transaction expense =
                new Transaction(
                        2L,
                        5L,
                        null,
                        null,
                        "Despesa",
                        BigDecimal.valueOf(800),
                        LocalDate.of(2026, 11, 1),
                        CategoryType.EXPENSE,
                        TransactionOrigin.MANUAL,
                        LocalDateTime.now(),
                        null,
                        null);
        when(transactionRepositoryPort.findAllByAccountIds(List.of(5L)))
                .thenReturn(List.of(income, expense));
        when(receiptGeneratorPort.generateIncomeStatement(any())).thenReturn(new byte[0]);

        service.generateIncomeStatement(
                10L, Year.of(2026), ReportGranularity.YEARLY, ReportFormat.PDF);

        ArgumentCaptor<IncomeStatementData> captor =
                ArgumentCaptor.forClass(IncomeStatementData.class);
        verify(receiptGeneratorPort).generateIncomeStatement(captor.capture());
        IncomeStatementData data = captor.getValue();

        assertThat(data.periods()).hasSize(1);
        assertThat(data.periods().get(0).label()).isEqualTo("2026");
        assertThat(data.periods().get(0).income()).isEqualByComparingTo("2000");
        assertThat(data.periods().get(0).expense()).isEqualByComparingTo("800");
        assertThat(data.periods().get(0).result()).isEqualByComparingTo("1200");
        assertThat(data.totalResult()).isEqualByComparingTo("1200");
    }

    @Test
    void generateIncomeStatementReturnsAllZeroTotalsWhenThereAreNoTransactions() {
        when(userRepositoryPort.findById(10L)).thenReturn(Optional.of(issuer()));
        when(accountRepositoryPort.findAllByUserId(10L)).thenReturn(List.of(ownedAccount()));
        when(transactionRepositoryPort.findAllByAccountIds(List.of(5L))).thenReturn(List.of());
        when(receiptGeneratorPort.generateIncomeStatement(any())).thenReturn(new byte[0]);

        service.generateIncomeStatement(
                10L, Year.of(2026), ReportGranularity.MONTHLY, ReportFormat.PDF);

        ArgumentCaptor<IncomeStatementData> captor =
                ArgumentCaptor.forClass(IncomeStatementData.class);
        verify(receiptGeneratorPort).generateIncomeStatement(captor.capture());
        IncomeStatementData data = captor.getValue();
        assertThat(data.totalIncome()).isEqualByComparingTo("0");
        assertThat(data.totalExpense()).isEqualByComparingTo("0");
        assertThat(data.totalResult()).isEqualByComparingTo("0");
        assertThat(data.periods())
                .allMatch(period -> period.result().compareTo(BigDecimal.ZERO) == 0);
    }

    @Test
    void generateBudgetVsActualReportComparesLimitAndSpentSortedByUsageDescending() {
        User issuer = issuer();
        when(userRepositoryPort.findById(10L)).thenReturn(Optional.of(issuer));

        Category rent = new Category(1L, 10L, "Aluguel", CategoryType.EXPENSE, null, null);
        Category food = new Category(2L, 10L, "Alimentação", CategoryType.EXPENSE, null, null);
        when(categoryRepositoryPort.findAllVisibleToUser(10L)).thenReturn(List.of(rent, food));

        Budget rentBudget =
                new Budget(1L, 10L, 1L, YearMonth.of(2026, 9), BigDecimal.valueOf(1000));
        Budget foodBudget = new Budget(2L, 10L, 2L, YearMonth.of(2026, 9), BigDecimal.valueOf(500));
        Budget otherMonthBudget =
                new Budget(3L, 10L, 1L, YearMonth.of(2026, 8), BigDecimal.valueOf(999));
        when(budgetRepositoryPort.findAllByUserId(10L))
                .thenReturn(List.of(rentBudget, foodBudget, otherMonthBudget));

        when(transactionRepositoryPort.sumAmountByUserIdAndCategoryIdAndTypeBetween(
                        10L,
                        1L,
                        CategoryType.EXPENSE,
                        LocalDate.of(2026, 9, 1),
                        LocalDate.of(2026, 10, 1)))
                .thenReturn(BigDecimal.valueOf(1200));
        when(transactionRepositoryPort.sumAmountByUserIdAndCategoryIdAndTypeBetween(
                        10L,
                        2L,
                        CategoryType.EXPENSE,
                        LocalDate.of(2026, 9, 1),
                        LocalDate.of(2026, 10, 1)))
                .thenReturn(BigDecimal.valueOf(100));
        when(receiptGeneratorPort.generateBudgetVsActualReport(any())).thenReturn(new byte[] {5});

        byte[] result =
                service.generateBudgetVsActualReport(10L, YearMonth.of(2026, 9), ReportFormat.PDF);

        assertThat(result).containsExactly(5);
        ArgumentCaptor<BudgetVsActualReportData> captor =
                ArgumentCaptor.forClass(BudgetVsActualReportData.class);
        verify(receiptGeneratorPort).generateBudgetVsActualReport(captor.capture());
        BudgetVsActualReportData data = captor.getValue();

        assertThat(data.comparisons()).hasSize(2);
        assertThat(data.comparisons())
                .extracting(BudgetVsActualReportData.BudgetComparison::categoryName)
                .containsExactly("Aluguel", "Alimentação");
        BudgetVsActualReportData.BudgetComparison rentComparison = data.comparisons().get(0);
        assertThat(rentComparison.limitValue()).isEqualByComparingTo("1000");
        assertThat(rentComparison.spentValue()).isEqualByComparingTo("1200");
        assertThat(rentComparison.difference()).isEqualByComparingTo("-200");
        assertThat(rentComparison.exceeded()).isTrue();
        BudgetVsActualReportData.BudgetComparison foodComparison = data.comparisons().get(1);
        assertThat(foodComparison.spentValue()).isEqualByComparingTo("100");
        assertThat(foodComparison.exceeded()).isFalse();
        assertThat(data.totalLimit()).isEqualByComparingTo("1500");
        assertThat(data.totalSpent()).isEqualByComparingTo("1300");
        assertThat(data.issuer()).isEqualTo(issuer);
        assertThat(data.referenceMonth()).isEqualTo(YearMonth.of(2026, 9));
    }

    @Test
    void generateBudgetVsActualReportUsesRemovedCategoryLabelWhenCategoryNoLongerExists() {
        when(userRepositoryPort.findById(10L)).thenReturn(Optional.of(issuer()));
        when(categoryRepositoryPort.findAllVisibleToUser(10L)).thenReturn(List.of());

        Budget budget = new Budget(1L, 10L, 999L, YearMonth.of(2026, 9), BigDecimal.valueOf(300));
        when(budgetRepositoryPort.findAllByUserId(10L)).thenReturn(List.of(budget));
        when(transactionRepositoryPort.sumAmountByUserIdAndCategoryIdAndTypeBetween(
                        any(), any(), any(), any(), any()))
                .thenReturn(BigDecimal.ZERO);
        when(receiptGeneratorPort.generateBudgetVsActualReport(any())).thenReturn(new byte[0]);

        service.generateBudgetVsActualReport(10L, YearMonth.of(2026, 9), ReportFormat.PDF);

        ArgumentCaptor<BudgetVsActualReportData> captor =
                ArgumentCaptor.forClass(BudgetVsActualReportData.class);
        verify(receiptGeneratorPort).generateBudgetVsActualReport(captor.capture());
        assertThat(captor.getValue().comparisons())
                .extracting(BudgetVsActualReportData.BudgetComparison::categoryName)
                .containsExactly("Categoria removida");
    }

    @Test
    void generateBudgetVsActualReportReturnsEmptyListWhenThereAreNoBudgetsInTheMonth() {
        when(userRepositoryPort.findById(10L)).thenReturn(Optional.of(issuer()));
        when(categoryRepositoryPort.findAllVisibleToUser(10L)).thenReturn(List.of());
        when(budgetRepositoryPort.findAllByUserId(10L)).thenReturn(List.of());
        when(receiptGeneratorPort.generateBudgetVsActualReport(any())).thenReturn(new byte[0]);

        service.generateBudgetVsActualReport(10L, YearMonth.of(2026, 9), ReportFormat.PDF);

        ArgumentCaptor<BudgetVsActualReportData> captor =
                ArgumentCaptor.forClass(BudgetVsActualReportData.class);
        verify(receiptGeneratorPort).generateBudgetVsActualReport(captor.capture());
        BudgetVsActualReportData data = captor.getValue();
        assertThat(data.comparisons()).isEmpty();
        assertThat(data.totalLimit()).isEqualByComparingTo("0");
        assertThat(data.totalSpent()).isEqualByComparingTo("0");
    }

    @Test
    void generateTransactionExportIncludesAllTransactionsInThePeriodAcrossAccountsSortedByDate() {
        Account checking =
                new Account(
                        5L,
                        10L,
                        "Conta Corrente",
                        AccountType.CHECKING,
                        BigDecimal.ZERO,
                        LocalDateTime.now());
        Account wallet =
                new Account(
                        6L,
                        10L,
                        "Carteira",
                        AccountType.WALLET,
                        BigDecimal.ZERO,
                        LocalDateTime.now());
        when(accountRepositoryPort.findAllByUserId(10L)).thenReturn(List.of(checking, wallet));

        Category rent = new Category(1L, 10L, "Aluguel", CategoryType.EXPENSE, null, null);
        when(categoryRepositoryPort.findAllVisibleToUser(10L)).thenReturn(List.of(rent));

        Client client = ownedClient();
        when(clientRepositoryPort.findAllByUserId(10L)).thenReturn(List.of(client));

        Transaction later =
                new Transaction(
                        1L,
                        5L,
                        1L,
                        null,
                        "Aluguel escritório",
                        BigDecimal.valueOf(1500),
                        LocalDate.of(2026, 9, 20),
                        CategoryType.EXPENSE,
                        TransactionOrigin.MANUAL,
                        LocalDateTime.now(),
                        null,
                        null);
        Transaction earlier =
                new Transaction(
                        2L,
                        6L,
                        null,
                        client.id(),
                        "Serviço prestado",
                        BigDecimal.valueOf(1000),
                        LocalDate.of(2026, 9, 5),
                        CategoryType.INCOME,
                        TransactionOrigin.MANUAL,
                        LocalDateTime.now(),
                        null,
                        null);
        Transaction outsidePeriod =
                new Transaction(
                        3L,
                        5L,
                        1L,
                        null,
                        "Aluguel de agosto",
                        BigDecimal.valueOf(1500),
                        LocalDate.of(2026, 8, 5),
                        CategoryType.EXPENSE,
                        TransactionOrigin.MANUAL,
                        LocalDateTime.now(),
                        null,
                        null);
        when(transactionRepositoryPort.findAllByAccountIds(List.of(5L, 6L)))
                .thenReturn(List.of(later, earlier, outsidePeriod));
        when(reportCsvExporterPort.exportTransactions(any())).thenReturn(new byte[] {7});

        byte[] result =
                service.generateTransactionExport(10L, YearMonth.of(2026, 9), ReportFormat.CSV);

        assertThat(result).containsExactly(7);
        ArgumentCaptor<TransactionExportData> captor =
                ArgumentCaptor.forClass(TransactionExportData.class);
        verify(reportCsvExporterPort).exportTransactions(captor.capture());
        TransactionExportData data = captor.getValue();

        assertThat(data.rows()).hasSize(2);
        assertThat(data.rows())
                .extracting(TransactionExportData.TransactionExportRow::description)
                .containsExactly("Serviço prestado", "Aluguel escritório");
        TransactionExportData.TransactionExportRow incomeRow = data.rows().get(0);
        assertThat(incomeRow.accountName()).isEqualTo("Carteira");
        assertThat(incomeRow.categoryName()).isEqualTo("Sem categoria");
        assertThat(incomeRow.clientName()).isEqualTo(client.name());
        TransactionExportData.TransactionExportRow expenseRow = data.rows().get(1);
        assertThat(expenseRow.accountName()).isEqualTo("Conta Corrente");
        assertThat(expenseRow.categoryName()).isEqualTo("Aluguel");
        assertThat(expenseRow.clientName()).isEmpty();
        assertThat(data.referenceMonth()).isEqualTo(YearMonth.of(2026, 9));
    }

    @Test
    void generateTransactionExportUsesRemovedLabelsWhenCategoryOrClientNoLongerExist() {
        Account account = ownedAccount();
        when(accountRepositoryPort.findAllByUserId(10L)).thenReturn(List.of(account));
        when(categoryRepositoryPort.findAllVisibleToUser(10L)).thenReturn(List.of());
        when(clientRepositoryPort.findAllByUserId(10L)).thenReturn(List.of());

        Transaction transaction =
                new Transaction(
                        1L,
                        5L,
                        999L,
                        888L,
                        "Transação antiga",
                        BigDecimal.valueOf(100),
                        LocalDate.of(2026, 9, 10),
                        CategoryType.EXPENSE,
                        TransactionOrigin.MANUAL,
                        LocalDateTime.now(),
                        null,
                        null);
        when(transactionRepositoryPort.findAllByAccountIds(List.of(5L)))
                .thenReturn(List.of(transaction));
        when(reportCsvExporterPort.exportTransactions(any())).thenReturn(new byte[0]);

        service.generateTransactionExport(10L, YearMonth.of(2026, 9), ReportFormat.CSV);

        ArgumentCaptor<TransactionExportData> captor =
                ArgumentCaptor.forClass(TransactionExportData.class);
        verify(reportCsvExporterPort).exportTransactions(captor.capture());
        TransactionExportData.TransactionExportRow row = captor.getValue().rows().get(0);
        assertThat(row.categoryName()).isEqualTo("Categoria removida");
        assertThat(row.clientName()).isEqualTo("Cliente removido");
    }

    @Test
    void generateTransactionExportReturnsEmptyListWhenThereAreNoTransactionsInThePeriod() {
        when(accountRepositoryPort.findAllByUserId(10L)).thenReturn(List.of(ownedAccount()));
        when(categoryRepositoryPort.findAllVisibleToUser(10L)).thenReturn(List.of());
        when(clientRepositoryPort.findAllByUserId(10L)).thenReturn(List.of());
        when(transactionRepositoryPort.findAllByAccountIds(List.of(5L))).thenReturn(List.of());
        when(reportCsvExporterPort.exportTransactions(any())).thenReturn(new byte[0]);

        service.generateTransactionExport(10L, YearMonth.of(2026, 9), ReportFormat.CSV);

        ArgumentCaptor<TransactionExportData> captor =
                ArgumentCaptor.forClass(TransactionExportData.class);
        verify(reportCsvExporterPort).exportTransactions(captor.capture());
        assertThat(captor.getValue().rows()).isEmpty();
    }

    @Test
    void generateTransactionExportCarriesTheStatusOfEachTransaction() {
        when(accountRepositoryPort.findAllByUserId(10L)).thenReturn(List.of(ownedAccount()));
        when(categoryRepositoryPort.findAllVisibleToUser(10L)).thenReturn(List.of());
        when(clientRepositoryPort.findAllByUserId(10L)).thenReturn(List.of());
        when(transactionRepositoryPort.findAllByAccountIds(List.of(5L)))
                .thenReturn(
                        List.of(
                                Transaction.create(
                                        5L,
                                        null,
                                        null,
                                        "Mensalidade",
                                        new BigDecimal("3000.00"),
                                        LocalDate.of(2026, 9, 20),
                                        CategoryType.INCOME,
                                        TransactionStatus.PENDING)));
        when(reportCsvExporterPort.exportTransactions(any())).thenReturn(new byte[0]);

        service.generateTransactionExport(10L, YearMonth.of(2026, 9), ReportFormat.CSV);

        ArgumentCaptor<TransactionExportData> captor =
                ArgumentCaptor.forClass(TransactionExportData.class);
        verify(reportCsvExporterPort).exportTransactions(captor.capture());
        assertThat(captor.getValue().rows())
                .extracting(TransactionExportData.TransactionExportRow::status)
                .containsExactly(TransactionStatus.PENDING);
    }

    @Test
    void generateClientReceiptUsesCsvExporterWhenFormatIsCsv() {
        when(clientRepositoryPort.findById(1L)).thenReturn(Optional.of(ownedClient()));
        when(userRepositoryPort.findById(10L)).thenReturn(Optional.of(issuer()));
        when(transactionRepositoryPort.findAllByClientIdAndTypeAndDateBetween(
                        any(), any(), any(), any()))
                .thenReturn(List.of());
        when(reportCsvExporterPort.exportClientReceipt(any())).thenReturn(new byte[] {1});

        byte[] result =
                service.generateClientReceipt(10L, 1L, YearMonth.of(2026, 9), ReportFormat.CSV);

        assertThat(result).containsExactly(1);
        verify(reportCsvExporterPort).exportClientReceipt(any(ClientReceiptData.class));
        verify(receiptGeneratorPort, never()).generateClientReceipt(any());
    }

    @Test
    void generateAccountStatementUsesCsvExporterWhenFormatIsCsv() {
        when(accountRepositoryPort.findById(5L)).thenReturn(Optional.of(ownedAccount()));
        when(userRepositoryPort.findById(10L)).thenReturn(Optional.of(issuer()));
        when(transactionRepositoryPort.findAllByAccountIds(List.of(5L))).thenReturn(List.of());
        when(reportCsvExporterPort.exportAccountStatement(any())).thenReturn(new byte[] {1});

        byte[] result =
                service.generateAccountStatement(10L, 5L, YearMonth.of(2026, 9), ReportFormat.CSV);

        assertThat(result).containsExactly(1);
        verify(reportCsvExporterPort).exportAccountStatement(any(AccountStatementData.class));
        verify(receiptGeneratorPort, never()).generateAccountStatement(any());
    }

    @Test
    void generateClientAnnualStatementUsesCsvExporterWhenFormatIsCsv() {
        when(clientRepositoryPort.findById(1L)).thenReturn(Optional.of(ownedClient()));
        when(userRepositoryPort.findById(10L)).thenReturn(Optional.of(issuer()));
        when(transactionRepositoryPort.findAllByClientIdAndTypeAndDateBetween(
                        any(), any(), any(), any()))
                .thenReturn(List.of());
        when(reportCsvExporterPort.exportClientAnnualStatement(any())).thenReturn(new byte[] {1});

        byte[] result =
                service.generateClientAnnualStatement(10L, 1L, Year.of(2026), ReportFormat.CSV);

        assertThat(result).containsExactly(1);
        verify(reportCsvExporterPort)
                .exportClientAnnualStatement(any(ClientAnnualStatementData.class));
        verify(receiptGeneratorPort, never()).generateClientAnnualStatement(any());
    }

    @Test
    void generateCategoryExpenseReportUsesCsvExporterWhenFormatIsCsv() {
        when(userRepositoryPort.findById(10L)).thenReturn(Optional.of(issuer()));
        when(accountRepositoryPort.findAllByUserId(10L)).thenReturn(List.of(ownedAccount()));
        when(categoryRepositoryPort.findAllVisibleToUser(10L)).thenReturn(List.of());
        when(transactionRepositoryPort.findAllByAccountIds(List.of(5L))).thenReturn(List.of());
        when(reportCsvExporterPort.exportCategoryExpenseReport(any())).thenReturn(new byte[] {1});

        byte[] result =
                service.generateCategoryExpenseReport(10L, YearMonth.of(2026, 9), ReportFormat.CSV);

        assertThat(result).containsExactly(1);
        verify(reportCsvExporterPort)
                .exportCategoryExpenseReport(any(CategoryExpenseReportData.class));
        verify(receiptGeneratorPort, never()).generateCategoryExpenseReport(any());
    }

    @Test
    void generateIncomeStatementUsesCsvExporterWhenFormatIsCsv() {
        when(userRepositoryPort.findById(10L)).thenReturn(Optional.of(issuer()));
        when(accountRepositoryPort.findAllByUserId(10L)).thenReturn(List.of(ownedAccount()));
        when(transactionRepositoryPort.findAllByAccountIds(List.of(5L))).thenReturn(List.of());
        when(reportCsvExporterPort.exportIncomeStatement(any())).thenReturn(new byte[] {1});

        byte[] result =
                service.generateIncomeStatement(
                        10L, Year.of(2026), ReportGranularity.MONTHLY, ReportFormat.CSV);

        assertThat(result).containsExactly(1);
        verify(reportCsvExporterPort).exportIncomeStatement(any(IncomeStatementData.class));
        verify(receiptGeneratorPort, never()).generateIncomeStatement(any());
    }

    @Test
    void generateBudgetVsActualReportUsesCsvExporterWhenFormatIsCsv() {
        when(userRepositoryPort.findById(10L)).thenReturn(Optional.of(issuer()));
        when(categoryRepositoryPort.findAllVisibleToUser(10L)).thenReturn(List.of());
        when(budgetRepositoryPort.findAllByUserId(10L)).thenReturn(List.of());
        when(reportCsvExporterPort.exportBudgetVsActualReport(any())).thenReturn(new byte[] {1});

        byte[] result =
                service.generateBudgetVsActualReport(10L, YearMonth.of(2026, 9), ReportFormat.CSV);

        assertThat(result).containsExactly(1);
        verify(reportCsvExporterPort)
                .exportBudgetVsActualReport(any(BudgetVsActualReportData.class));
        verify(receiptGeneratorPort, never()).generateBudgetVsActualReport(any());
    }

    @Test
    void generateTransactionExportUsesPdfGeneratorWhenFormatIsPdf() {
        when(accountRepositoryPort.findAllByUserId(10L)).thenReturn(List.of(ownedAccount()));
        when(categoryRepositoryPort.findAllVisibleToUser(10L)).thenReturn(List.of());
        when(clientRepositoryPort.findAllByUserId(10L)).thenReturn(List.of());
        when(transactionRepositoryPort.findAllByAccountIds(List.of(5L))).thenReturn(List.of());
        when(receiptGeneratorPort.generateTransactionExport(any())).thenReturn(new byte[] {1});

        byte[] result =
                service.generateTransactionExport(10L, YearMonth.of(2026, 9), ReportFormat.PDF);

        assertThat(result).containsExactly(1);
        verify(receiptGeneratorPort).generateTransactionExport(any(TransactionExportData.class));
        verify(reportCsvExporterPort, never()).exportTransactions(any());
    }

    private static BigDecimal monthlyTotal(ClientAnnualStatementData data, Month month) {
        return data.monthlyIncomes().stream()
                .filter(monthly -> monthly.month() == month)
                .findFirst()
                .orElseThrow()
                .total();
    }
}
