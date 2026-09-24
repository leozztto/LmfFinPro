package com.lmf.finpro.infrastructure.csv;

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
import java.util.List;
import org.junit.jupiter.api.Test;

class ReportCsvExporterTest {

    private final ReportCsvExporter exporter = new ReportCsvExporter();

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

    private static Client client() {
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

    private static Account account() {
        return new Account(
                5L,
                10L,
                "Conta Corrente",
                AccountType.CHECKING,
                new BigDecimal("1000.00"),
                LocalDateTime.now());
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

    private static String bodyOf(byte[] csv) {
        assertThat(csv[0]).isEqualTo((byte) 0xEF);
        assertThat(csv[1]).isEqualTo((byte) 0xBB);
        assertThat(csv[2]).isEqualTo((byte) 0xBF);
        return new String(csv, 3, csv.length - 3, StandardCharsets.UTF_8);
    }

    @Test
    void exportsClientReceiptWithTotalRow() {
        ClientReceiptData data =
                new ClientReceiptData(
                        issuer(),
                        client(),
                        YearMonth.of(2026, 9),
                        List.of(
                                transaction(
                                        "Serviço A",
                                        "1000.00",
                                        LocalDate.of(2026, 9, 5),
                                        CategoryType.INCOME)),
                        new BigDecimal("1000.00"));

        String content = bodyOf(exporter.exportClientReceipt(data));

        assertThat(content)
                .isEqualTo(
                        "Data;Descrição;Valor\r\n"
                                + "2026-09-05;Serviço A;1000.00\r\n"
                                + ";TOTAL;1000.00\r\n");
    }

    @Test
    void exportsAccountStatementWithOpeningAndClosingBalanceRows() {
        AccountStatementData data =
                new AccountStatementData(
                        issuer(),
                        account(),
                        YearMonth.of(2026, 9),
                        new BigDecimal("1000.00"),
                        List.of(
                                transaction(
                                        "Receita do mês",
                                        "500.00",
                                        LocalDate.of(2026, 9, 5),
                                        CategoryType.INCOME),
                                transaction(
                                        "Despesa do mês",
                                        "200.00",
                                        LocalDate.of(2026, 9, 10),
                                        CategoryType.EXPENSE)),
                        new BigDecimal("500.00"),
                        new BigDecimal("200.00"),
                        new BigDecimal("1300.00"));

        String content = bodyOf(exporter.exportAccountStatement(data));

        assertThat(content)
                .isEqualTo(
                        "Data;Descrição;Tipo;Valor;Saldo\r\n"
                                + ";Saldo de abertura;;;1000.00\r\n"
                                + "2026-09-05;Receita do mês;Receita;500.00;1500.00\r\n"
                                + "2026-09-10;Despesa do mês;Despesa;-200.00;1300.00\r\n"
                                + ";Saldo final do período;;;1300.00\r\n");
    }

    @Test
    void exportsClientAnnualStatementWithAllTwelveMonths() {
        List<ClientAnnualStatementData.MonthlyIncome> monthlyIncomes =
                List.of(
                        new ClientAnnualStatementData.MonthlyIncome(
                                Month.JANUARY, new BigDecimal("1000.00")),
                        new ClientAnnualStatementData.MonthlyIncome(
                                Month.FEBRUARY, BigDecimal.ZERO));
        ClientAnnualStatementData data =
                new ClientAnnualStatementData(
                        issuer(),
                        client(),
                        Year.of(2026),
                        monthlyIncomes,
                        new BigDecimal("1000.00"));

        String content = bodyOf(exporter.exportClientAnnualStatement(data));

        assertThat(content)
                .isEqualTo(
                        "Mês;Valor recebido\r\n"
                                + "Janeiro;1000.00\r\n"
                                + "Fevereiro;0\r\n"
                                + "TOTAL;1000.00\r\n");
    }

    @Test
    void exportsCategoryExpenseReportWithTotalRow() {
        CategoryExpenseReportData data =
                new CategoryExpenseReportData(
                        issuer(),
                        YearMonth.of(2026, 9),
                        List.of(
                                new CategoryExpenseReportData.CategoryExpense(
                                        "Aluguel", new BigDecimal("1500.00"))),
                        new BigDecimal("1500.00"));

        String content = bodyOf(exporter.exportCategoryExpenseReport(data));

        assertThat(content)
                .isEqualTo(
                        "Categoria;Valor gasto\r\n" + "Aluguel;1500.00\r\n" + "TOTAL;1500.00\r\n");
    }

    @Test
    void exportsIncomeStatementWithTotalRow() {
        IncomeStatementData data =
                new IncomeStatementData(
                        issuer(),
                        Year.of(2026),
                        ReportGranularity.YEARLY,
                        List.of(
                                new IncomeStatementData.PeriodResult(
                                        "2026",
                                        new BigDecimal("2000.00"),
                                        new BigDecimal("800.00"),
                                        new BigDecimal("1200.00"))),
                        new BigDecimal("2000.00"),
                        new BigDecimal("800.00"),
                        new BigDecimal("1200.00"));

        String content = bodyOf(exporter.exportIncomeStatement(data));

        assertThat(content)
                .isEqualTo(
                        "Período;Receita;Despesa;Resultado\r\n"
                                + "2026;2000.00;800.00;1200.00\r\n"
                                + "TOTAL;2000.00;800.00;1200.00\r\n");
    }

    @Test
    void exportsBudgetVsActualReportWithTotalRow() {
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
                                        true)),
                        new BigDecimal("1000.00"),
                        new BigDecimal("1200.00"));

        String content = bodyOf(exporter.exportBudgetVsActualReport(data));

        assertThat(content)
                .isEqualTo(
                        "Categoria;Limite;Gasto real;Diferença\r\n"
                                + "Aluguel;1000.00;1200.00;-200.00\r\n"
                                + "TOTAL;1000.00;1200.00;-200.00\r\n");
    }

    @Test
    void exportsTransactionsWithDelimiterSemicolonAndBom() {
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

        String content = bodyOf(exporter.exportTransactions(data));

        assertThat(content)
                .isEqualTo(
                        "Data;Conta;Categoria;Cliente;Descrição;Tipo;Valor\r\n"
                                + "2026-09-05;Conta Corrente;Aluguel;;Aluguel escritório;Despesa;1500.00\r\n");
    }

    @Test
    void quotesFieldsThatContainTheDelimiterOrQuotes() {
        CategoryExpenseReportData data =
                new CategoryExpenseReportData(
                        issuer(),
                        YearMonth.of(2026, 9),
                        List.of(
                                new CategoryExpenseReportData.CategoryExpense(
                                        "Categoria; com \"aspas\"", new BigDecimal("10.00"))),
                        new BigDecimal("10.00"));

        String content = bodyOf(exporter.exportCategoryExpenseReport(data));

        assertThat(content).contains("\"Categoria; com \"\"aspas\"\"\"");
    }
}
