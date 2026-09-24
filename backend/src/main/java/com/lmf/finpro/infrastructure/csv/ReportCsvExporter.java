package com.lmf.finpro.infrastructure.csv;

import com.lmf.finpro.domain.model.AccountStatementData;
import com.lmf.finpro.domain.model.BudgetVsActualReportData;
import com.lmf.finpro.domain.model.CategoryExpenseReportData;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.ClientAnnualStatementData;
import com.lmf.finpro.domain.model.ClientReceiptData;
import com.lmf.finpro.domain.model.IncomeStatementData;
import com.lmf.finpro.domain.model.Transaction;
import com.lmf.finpro.domain.model.TransactionExportData;
import com.lmf.finpro.domain.port.out.ReportCsvExporterPort;
import java.math.BigDecimal;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Monta, na mão, a versão CSV de cada relatório — mesmos dados já calculados pelo {@code
 * ReportApplicationService} para o PDF, só que em tabela simples, uma linha de total ao final.
 * Reaproveita {@link CsvWriter} para o BOM UTF-8, delimitador {@code ;} e escaping compartilhados
 * entre todos os relatórios.
 */
@Component
public class ReportCsvExporter implements ReportCsvExporterPort {

    private static final Locale PT_BR = Locale.of("pt", "BR");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    @Override
    public byte[] exportClientReceipt(ClientReceiptData data) {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[] {"Data", "Descrição", "Valor"});
        for (Transaction transaction : data.transactions()) {
            rows.add(
                    new String[] {
                        transaction.transactionDate().format(DATE_FORMAT),
                        transaction.description(),
                        transaction.amount().toPlainString()
                    });
        }
        rows.add(new String[] {"", "TOTAL", data.total().toPlainString()});
        return CsvWriter.write(rows);
    }

    @Override
    public byte[] exportAccountStatement(AccountStatementData data) {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[] {"Data", "Descrição", "Tipo", "Valor", "Saldo"});
        rows.add(
                new String[] {
                    "", "Saldo de abertura", "", "", data.openingBalance().toPlainString()
                });

        BigDecimal runningBalance = data.openingBalance();
        for (Transaction transaction : data.transactions()) {
            boolean isIncome = transaction.type() == CategoryType.INCOME;
            runningBalance =
                    isIncome
                            ? runningBalance.add(transaction.amount())
                            : runningBalance.subtract(transaction.amount());
            rows.add(
                    new String[] {
                        transaction.transactionDate().format(DATE_FORMAT),
                        transaction.description(),
                        isIncome ? "Receita" : "Despesa",
                        (isIncome ? "" : "-") + transaction.amount().toPlainString(),
                        runningBalance.toPlainString()
                    });
        }
        rows.add(
                new String[] {
                    "", "Saldo final do período", "", "", data.closingBalance().toPlainString()
                });
        return CsvWriter.write(rows);
    }

    @Override
    public byte[] exportClientAnnualStatement(ClientAnnualStatementData data) {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[] {"Mês", "Valor recebido"});
        for (ClientAnnualStatementData.MonthlyIncome monthlyIncome : data.monthlyIncomes()) {
            rows.add(
                    new String[] {
                        capitalizeMonth(monthlyIncome.month()),
                        monthlyIncome.total().toPlainString()
                    });
        }
        rows.add(new String[] {"TOTAL", data.totalYear().toPlainString()});
        return CsvWriter.write(rows);
    }

    @Override
    public byte[] exportCategoryExpenseReport(CategoryExpenseReportData data) {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[] {"Categoria", "Valor gasto"});
        for (CategoryExpenseReportData.CategoryExpense categoryExpense : data.categoryExpenses()) {
            rows.add(
                    new String[] {
                        categoryExpense.categoryName(), categoryExpense.total().toPlainString()
                    });
        }
        rows.add(new String[] {"TOTAL", data.totalExpense().toPlainString()});
        return CsvWriter.write(rows);
    }

    @Override
    public byte[] exportIncomeStatement(IncomeStatementData data) {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[] {"Período", "Receita", "Despesa", "Resultado"});
        for (IncomeStatementData.PeriodResult period : data.periods()) {
            rows.add(
                    new String[] {
                        period.label(),
                        period.income().toPlainString(),
                        period.expense().toPlainString(),
                        period.result().toPlainString()
                    });
        }
        rows.add(
                new String[] {
                    "TOTAL",
                    data.totalIncome().toPlainString(),
                    data.totalExpense().toPlainString(),
                    data.totalResult().toPlainString()
                });
        return CsvWriter.write(rows);
    }

    @Override
    public byte[] exportBudgetVsActualReport(BudgetVsActualReportData data) {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[] {"Categoria", "Limite", "Gasto real", "Diferença"});
        for (BudgetVsActualReportData.BudgetComparison comparison : data.comparisons()) {
            rows.add(
                    new String[] {
                        comparison.categoryName(),
                        comparison.limitValue().toPlainString(),
                        comparison.spentValue().toPlainString(),
                        comparison.difference().toPlainString()
                    });
        }
        rows.add(
                new String[] {
                    "TOTAL",
                    data.totalLimit().toPlainString(),
                    data.totalSpent().toPlainString(),
                    data.totalLimit().subtract(data.totalSpent()).toPlainString()
                });
        return CsvWriter.write(rows);
    }

    @Override
    public byte[] exportTransactions(TransactionExportData data) {
        List<String[]> rows = new ArrayList<>();
        rows.add(
                new String[] {
                    "Data", "Conta", "Categoria", "Cliente", "Descrição", "Tipo", "Valor"
                });
        for (TransactionExportData.TransactionExportRow row : data.rows()) {
            rows.add(
                    new String[] {
                        row.date().format(DATE_FORMAT),
                        row.accountName(),
                        row.categoryName(),
                        row.clientName(),
                        row.description(),
                        row.type() == CategoryType.INCOME ? "Receita" : "Despesa",
                        row.amount().toPlainString()
                    });
        }
        return CsvWriter.write(rows);
    }

    private String capitalizeMonth(Month month) {
        String name = month.getDisplayName(TextStyle.FULL, PT_BR);
        return name.substring(0, 1).toUpperCase(PT_BR) + name.substring(1);
    }
}
