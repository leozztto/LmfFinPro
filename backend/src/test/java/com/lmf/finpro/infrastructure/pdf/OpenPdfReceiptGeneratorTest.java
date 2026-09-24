package com.lmf.finpro.infrastructure.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import com.lmf.finpro.domain.model.Account;
import com.lmf.finpro.domain.model.AccountStatementData;
import com.lmf.finpro.domain.model.AccountType;
import com.lmf.finpro.domain.model.Address;
import com.lmf.finpro.domain.model.BrazilianState;
import com.lmf.finpro.domain.model.BudgetVsActualReportData;
import com.lmf.finpro.domain.model.CategoryExpenseReportData;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.Client;
import com.lmf.finpro.domain.model.ClientAnnualStatementData;
import com.lmf.finpro.domain.model.ClientReceiptData;
import com.lmf.finpro.domain.model.ClientWorkType;
import com.lmf.finpro.domain.model.DocumentType;
import com.lmf.finpro.domain.model.IncomeStatementData;
import com.lmf.finpro.domain.model.ReportGranularity;
import com.lmf.finpro.domain.model.TaxRegime;
import com.lmf.finpro.domain.model.Transaction;
import com.lmf.finpro.domain.model.TransactionExportData;
import com.lmf.finpro.domain.model.TransactionOrigin;
import com.lmf.finpro.domain.model.User;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.Year;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class OpenPdfReceiptGeneratorTest {

    private final OpenPdfReceiptGenerator generator = new OpenPdfReceiptGenerator();

    private static User issuer() {
        Address address =
                new Address(
                        "01310100",
                        "Av. Paulista",
                        "1000",
                        "Sala 1",
                        "Bela Vista",
                        "São Paulo",
                        BrazilianState.SP);
        return new User(
                10L,
                "Prestador de Serviço",
                "prestador@x.com",
                "hash",
                DocumentType.CPF,
                "11144477735",
                "11999998888",
                TaxRegime.AUTONOMO,
                address,
                LocalDateTime.now());
    }

    private static User issuerWithoutAddress() {
        return new User(
                10L,
                "Prestador de Serviço",
                "prestador@x.com",
                "hash",
                DocumentType.CPF,
                "11144477735",
                "11999998888",
                TaxRegime.AUTONOMO,
                null,
                LocalDateTime.now());
    }

    private static Client client() {
        return new Client(
                1L,
                10L,
                "Empresa Cliente",
                "contato@cliente.com",
                "11987654321",
                DocumentType.CNPJ,
                "11444777000161",
                ClientWorkType.PJ,
                null,
                null,
                true);
    }

    private static Transaction transaction(String description, String amount, LocalDate date) {
        return transaction(description, amount, date, CategoryType.INCOME);
    }

    private static Transaction transaction(
            String description, String amount, LocalDate date, CategoryType type) {
        return new Transaction(
                1L,
                5L,
                null,
                1L,
                description,
                new BigDecimal(amount),
                date,
                type,
                TransactionOrigin.MANUAL,
                LocalDateTime.now(),
                null,
                null);
    }

    private static Account account() {
        return new Account(
                5L,
                10L,
                "Conta Corrente",
                AccountType.CHECKING,
                new BigDecimal("1000.00"),
                LocalDateTime.now());
    }

    @Test
    void producesAPdfDocumentStartingWithThePdfMagicBytes() {
        ClientReceiptData data =
                new ClientReceiptData(
                        issuer(),
                        client(),
                        YearMonth.of(2026, 9),
                        List.of(
                                transaction(
                                        "Serviço de consultoria",
                                        "1000.00",
                                        LocalDate.of(2026, 9, 5)),
                                transaction(
                                        "Manutenção mensal", "500.00", LocalDate.of(2026, 9, 20))),
                        new BigDecimal("1500.00"));

        byte[] pdf = generator.generateClientReceipt(data);

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 4, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF");
    }

    @Test
    void producesAPdfEvenWithNoTransactionsInThePeriod() {
        ClientReceiptData data =
                new ClientReceiptData(
                        issuer(), client(), YearMonth.of(2026, 9), List.of(), BigDecimal.ZERO);

        byte[] pdf = generator.generateClientReceipt(data);

        assertThat(new String(pdf, 0, 4, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF");
    }

    @Test
    void producesAPdfEvenWhenTheIssuerHasNoAddress() {
        ClientReceiptData data =
                new ClientReceiptData(
                        issuerWithoutAddress(),
                        client(),
                        YearMonth.of(2026, 9),
                        List.of(),
                        BigDecimal.ZERO);

        byte[] pdf = generator.generateClientReceipt(data);

        assertThat(new String(pdf, 0, 4, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF");
    }

    @Test
    void producesAnAccountStatementPdfDocumentStartingWithThePdfMagicBytes() {
        AccountStatementData data =
                new AccountStatementData(
                        issuer(),
                        account(),
                        YearMonth.of(2026, 9),
                        new BigDecimal("1000.00"),
                        List.of(
                                transaction(
                                        "Receita do mês",
                                        "1000.00",
                                        LocalDate.of(2026, 9, 5),
                                        CategoryType.INCOME),
                                transaction(
                                        "Despesa do mês",
                                        "300.00",
                                        LocalDate.of(2026, 9, 20),
                                        CategoryType.EXPENSE)),
                        new BigDecimal("1000.00"),
                        new BigDecimal("300.00"),
                        new BigDecimal("1700.00"));

        byte[] pdf = generator.generateAccountStatement(data);

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 4, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF");
    }

    @Test
    void producesAnAccountStatementPdfEvenWithNoTransactionsInThePeriod() {
        AccountStatementData data =
                new AccountStatementData(
                        issuer(),
                        account(),
                        YearMonth.of(2026, 9),
                        new BigDecimal("1000.00"),
                        List.of(),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        new BigDecimal("1000.00"));

        byte[] pdf = generator.generateAccountStatement(data);

        assertThat(new String(pdf, 0, 4, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF");
    }

    @Test
    void producesAClientAnnualStatementPdfDocumentStartingWithThePdfMagicBytes() {
        List<ClientAnnualStatementData.MonthlyIncome> monthlyIncomes =
                Arrays.stream(Month.values())
                        .map(
                                month ->
                                        new ClientAnnualStatementData.MonthlyIncome(
                                                month,
                                                month == Month.JANUARY
                                                        ? new BigDecimal("1000.00")
                                                        : BigDecimal.ZERO))
                        .toList();
        ClientAnnualStatementData data =
                new ClientAnnualStatementData(
                        issuer(),
                        client(),
                        Year.of(2026),
                        monthlyIncomes,
                        new BigDecimal("1000.00"));

        byte[] pdf = generator.generateClientAnnualStatement(data);

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 4, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF");
    }

    @Test
    void producesACategoryExpenseReportPdfDocumentStartingWithThePdfMagicBytes() {
        CategoryExpenseReportData data =
                new CategoryExpenseReportData(
                        issuer(),
                        YearMonth.of(2026, 9),
                        List.of(
                                new CategoryExpenseReportData.CategoryExpense(
                                        "Aluguel", new BigDecimal("1500.00")),
                                new CategoryExpenseReportData.CategoryExpense(
                                        "Software", new BigDecimal("100.00"))),
                        new BigDecimal("1600.00"));

        byte[] pdf = generator.generateCategoryExpenseReport(data);

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 4, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF");
    }

    @Test
    void producesACategoryExpenseReportPdfEvenWithNoExpensesInThePeriod() {
        CategoryExpenseReportData data =
                new CategoryExpenseReportData(
                        issuer(), YearMonth.of(2026, 9), List.of(), BigDecimal.ZERO);

        byte[] pdf = generator.generateCategoryExpenseReport(data);

        assertThat(new String(pdf, 0, 4, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF");
    }

    @Test
    void producesAnIncomeStatementPdfDocumentStartingWithThePdfMagicBytes() {
        List<IncomeStatementData.PeriodResult> periods =
                Arrays.stream(Month.values())
                        .map(
                                month ->
                                        new IncomeStatementData.PeriodResult(
                                                month == Month.JANUARY ? "Janeiro" : month.name(),
                                                month == Month.JANUARY
                                                        ? new BigDecimal("1000.00")
                                                        : BigDecimal.ZERO,
                                                month == Month.JANUARY
                                                        ? new BigDecimal("300.00")
                                                        : BigDecimal.ZERO,
                                                month == Month.JANUARY
                                                        ? new BigDecimal("700.00")
                                                        : BigDecimal.ZERO))
                        .toList();
        IncomeStatementData data =
                new IncomeStatementData(
                        issuer(),
                        Year.of(2026),
                        ReportGranularity.MONTHLY,
                        periods,
                        new BigDecimal("1000.00"),
                        new BigDecimal("300.00"),
                        new BigDecimal("700.00"));

        byte[] pdf = generator.generateIncomeStatement(data);

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 4, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF");
    }

    @Test
    void producesAnIncomeStatementPdfForQuarterlyAndYearlyGranularities() {
        IncomeStatementData quarterly =
                new IncomeStatementData(
                        issuer(),
                        Year.of(2026),
                        ReportGranularity.QUARTERLY,
                        List.of(
                                new IncomeStatementData.PeriodResult(
                                        "1º trimestre",
                                        BigDecimal.ZERO,
                                        BigDecimal.ZERO,
                                        BigDecimal.ZERO)),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO);
        IncomeStatementData yearly =
                new IncomeStatementData(
                        issuer(),
                        Year.of(2026),
                        ReportGranularity.YEARLY,
                        List.of(
                                new IncomeStatementData.PeriodResult(
                                        "2026", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO);

        assertThat(
                        new String(
                                generator.generateIncomeStatement(quarterly),
                                0,
                                4,
                                StandardCharsets.ISO_8859_1))
                .isEqualTo("%PDF");
        assertThat(
                        new String(
                                generator.generateIncomeStatement(yearly),
                                0,
                                4,
                                StandardCharsets.ISO_8859_1))
                .isEqualTo("%PDF");
    }

    @Test
    void producesABudgetVsActualReportPdfDocumentStartingWithThePdfMagicBytes() {
        BudgetVsActualReportData data =
                new BudgetVsActualReportData(
                        issuer(),
                        YearMonth.of(2026, 9),
                        List.of(
                                new BudgetVsActualReportData.BudgetComparison(
                                        "Aluguel",
                                        new BigDecimal("1000.00"),
                                        new BigDecimal("1200.00"),
                                        new BigDecimal("-200.00"),
                                        true),
                                new BudgetVsActualReportData.BudgetComparison(
                                        "Alimentação",
                                        new BigDecimal("500.00"),
                                        new BigDecimal("100.00"),
                                        new BigDecimal("400.00"),
                                        false)),
                        new BigDecimal("1500.00"),
                        new BigDecimal("1300.00"));

        byte[] pdf = generator.generateBudgetVsActualReport(data);

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 4, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF");
    }

    @Test
    void producesABudgetVsActualReportPdfEvenWithNoBudgetsInTheMonth() {
        BudgetVsActualReportData data =
                new BudgetVsActualReportData(
                        issuer(),
                        YearMonth.of(2026, 9),
                        List.of(),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO);

        byte[] pdf = generator.generateBudgetVsActualReport(data);

        assertThat(new String(pdf, 0, 4, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF");
    }

    @Test
    void producesATransactionExportPdfDocumentStartingWithThePdfMagicBytes() {
        TransactionExportData data =
                new TransactionExportData(
                        YearMonth.of(2026, 9),
                        List.of(
                                new TransactionExportData.TransactionExportRow(
                                        LocalDate.of(2026, 9, 5),
                                        "Conta Corrente",
                                        "Aluguel",
                                        "",
                                        "Aluguel escritório",
                                        CategoryType.EXPENSE,
                                        new BigDecimal("1500.00"))));

        byte[] pdf = generator.generateTransactionExport(data);

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 4, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF");
    }

    @Test
    void producesATransactionExportPdfEvenWithNoTransactionsInThePeriod() {
        TransactionExportData data = new TransactionExportData(YearMonth.of(2026, 9), List.of());

        byte[] pdf = generator.generateTransactionExport(data);

        assertThat(new String(pdf, 0, 4, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF");
    }
}
