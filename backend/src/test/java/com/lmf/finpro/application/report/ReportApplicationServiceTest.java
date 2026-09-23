package com.lmf.finpro.application.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lmf.finpro.domain.exception.ResourceNotFoundException;
import com.lmf.finpro.domain.model.Account;
import com.lmf.finpro.domain.model.AccountStatementData;
import com.lmf.finpro.domain.model.AccountType;
import com.lmf.finpro.domain.model.Address;
import com.lmf.finpro.domain.model.BrazilianState;
import com.lmf.finpro.domain.model.Category;
import com.lmf.finpro.domain.model.CategoryExpenseReportData;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.Client;
import com.lmf.finpro.domain.model.ClientAnnualStatementData;
import com.lmf.finpro.domain.model.ClientReceiptData;
import com.lmf.finpro.domain.model.ClientWorkType;
import com.lmf.finpro.domain.model.DocumentType;
import com.lmf.finpro.domain.model.TaxRegime;
import com.lmf.finpro.domain.model.Transaction;
import com.lmf.finpro.domain.model.TransactionOrigin;
import com.lmf.finpro.domain.model.User;
import com.lmf.finpro.domain.port.out.AccountRepositoryPort;
import com.lmf.finpro.domain.port.out.CategoryRepositoryPort;
import com.lmf.finpro.domain.port.out.ClientRepositoryPort;
import com.lmf.finpro.domain.port.out.ReceiptGeneratorPort;
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
    @Mock private ReceiptGeneratorPort receiptGeneratorPort;

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
                LocalDateTime.now());
    }

    @Test
    void throwsWhenClientDoesNotBelongToCurrentUser() {
        when(clientRepositoryPort.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generateClientReceipt(10L, 1L, YearMonth.of(2026, 9)))
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

        assertThatThrownBy(() -> service.generateClientReceipt(10L, 1L, YearMonth.of(2026, 9)))
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

        byte[] result = service.generateClientReceipt(10L, 1L, YearMonth.of(2026, 9));

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

        service.generateClientReceipt(10L, 1L, YearMonth.of(2026, 9));

        ArgumentCaptor<ClientReceiptData> captor = ArgumentCaptor.forClass(ClientReceiptData.class);
        verify(receiptGeneratorPort).generateClientReceipt(captor.capture());
        assertThat(captor.getValue().total()).isEqualByComparingTo("0");
    }

    @Test
    void generateAccountStatementThrowsWhenAccountDoesNotBelongToCurrentUser() {
        when(accountRepositoryPort.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generateAccountStatement(10L, 5L, YearMonth.of(2026, 9)))
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

        assertThatThrownBy(() -> service.generateAccountStatement(10L, 5L, YearMonth.of(2026, 9)))
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

        byte[] result = service.generateAccountStatement(10L, 5L, YearMonth.of(2026, 9));

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

        service.generateAccountStatement(10L, 5L, YearMonth.of(2026, 9));

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

        assertThatThrownBy(() -> service.generateClientAnnualStatement(10L, 1L, Year.of(2026)))
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

        byte[] result = service.generateClientAnnualStatement(10L, 1L, Year.of(2026));

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

        service.generateClientAnnualStatement(10L, 1L, Year.of(2026));

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

        byte[] result = service.generateCategoryExpenseReport(10L, YearMonth.of(2026, 9));

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

        service.generateCategoryExpenseReport(10L, YearMonth.of(2026, 9));

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

        service.generateCategoryExpenseReport(10L, YearMonth.of(2026, 9));

        ArgumentCaptor<CategoryExpenseReportData> captor =
                ArgumentCaptor.forClass(CategoryExpenseReportData.class);
        verify(receiptGeneratorPort).generateCategoryExpenseReport(captor.capture());
        CategoryExpenseReportData data = captor.getValue();
        assertThat(data.categoryExpenses()).isEmpty();
        assertThat(data.totalExpense()).isEqualByComparingTo("0");
    }

    private static BigDecimal monthlyTotal(ClientAnnualStatementData data, Month month) {
        return data.monthlyIncomes().stream()
                .filter(monthly -> monthly.month() == month)
                .findFirst()
                .orElseThrow()
                .total();
    }
}
