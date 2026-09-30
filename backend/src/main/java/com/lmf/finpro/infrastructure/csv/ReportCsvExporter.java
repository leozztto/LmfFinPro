package com.lmf.finpro.infrastructure.csv;

import com.lmf.finpro.domain.model.AccountStatementData;
import com.lmf.finpro.domain.model.BudgetVsActualReportData;
import com.lmf.finpro.domain.model.CategoryExpenseReportData;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.ClientAnnualStatementData;
import com.lmf.finpro.domain.model.ClientReceiptData;
import com.lmf.finpro.domain.model.DebtType;
import com.lmf.finpro.domain.model.IncomeStatementData;
import com.lmf.finpro.domain.model.Money;
import com.lmf.finpro.domain.model.NetWorthCalculator;
import com.lmf.finpro.domain.model.NetWorthReportData;
import com.lmf.finpro.domain.model.TagTotalsReportData;
import com.lmf.finpro.domain.model.Transaction;
import com.lmf.finpro.domain.model.TransactionExportData;
import com.lmf.finpro.domain.model.TransactionReportData;
import com.lmf.finpro.domain.model.TransactionStatus;
import com.lmf.finpro.domain.port.out.ReportCsvExporterPort;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
                        transaction.baseAmount().toPlainString()
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
                    "Data",
                    "Conta",
                    "Categoria",
                    "Cliente",
                    "Descrição",
                    "Tipo",
                    "Situação",
                    "Valor",
                    "Tags",
                    "Moeda original",
                    "Valor original"
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
                        row.status() == TransactionStatus.PAID ? "Paga" : "Pendente",
                        row.amount().toPlainString(),
                        row.tags(),
                        foreignCurrency(row.foreignValue()),
                        foreignAmount(row.foreignValue())
                    });
        }
        return CsvWriter.write(rows);
    }

    /**
     * Uma linha por transação e, ao final, os totais geral, pago e pendente. As tags vão na última
     * coluna, para o valor continuar na mesma posição de antes.
     */
    @Override
    public byte[] exportTransactionReport(TransactionReportData data) {
        List<String[]> rows = new ArrayList<>();
        rows.add(
                new String[] {
                    "Data",
                    "Descrição",
                    "Conta",
                    "Categoria",
                    "Cliente",
                    "Situação",
                    "Valor",
                    "Tags",
                    "Moeda original",
                    "Valor original"
                });
        for (TransactionReportData.Row row : data.rows()) {
            rows.add(
                    new String[] {
                        row.date().format(DATE_FORMAT),
                        row.description(),
                        row.accountName(),
                        row.categoryName(),
                        row.clientName(),
                        row.status() == TransactionStatus.PAID ? "Paga" : "Pendente",
                        row.amount().toPlainString(),
                        row.tags(),
                        foreignCurrency(row.foreignValue()),
                        foreignAmount(row.foreignValue())
                    });
        }
        rows.add(
                new String[] {
                    "", "TOTAL", "", "", "", "", data.total().toPlainString(), "", "", ""
                });
        rows.add(
                new String[] {
                    "", "Total pago", "", "", "", "", data.paidTotal().toPlainString(), "", "", ""
                });
        rows.add(
                new String[] {
                    "",
                    "Total pendente",
                    "",
                    "",
                    "",
                    "",
                    data.pendingTotal().toPlainString(),
                    "",
                    "",
                    ""
                });
        return CsvWriter.write(rows);
    }

    /**
     * Uma linha por tag e, sem filtro de tag, a linha "Sem tag". Sem linha de total: uma transação
     * com duas tags entra nas duas.
     */
    @Override
    public byte[] exportTagTotalsReport(TagTotalsReportData data) {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[] {"Tag", "Lançamentos", "Receitas", "Despesas", "Resultado"});
        List<TagTotalsReportData.Row> lines = new ArrayList<>(data.rows());
        if (data.untagged() != null) {
            lines.add(data.untagged());
        }
        for (TagTotalsReportData.Row row : lines) {
            rows.add(
                    new String[] {
                        row.label(),
                        String.valueOf(row.count()),
                        row.income().toPlainString(),
                        row.expense().toPlainString(),
                        row.result().toPlainString()
                    });
        }
        return CsvWriter.write(rows);
    }

    /**
     * A evolução mês a mês vem primeiro (é a tabela que vira gráfico na planilha); abaixo,
     * separadas por uma linha em branco, a composição de hoje: contas, investimentos e dívidas.
     * Valores monetários sem formatação, em reais, exceto as colunas "Moeda" e "Saldo na moeda".
     */
    @Override
    public byte[] exportNetWorthReport(NetWorthReportData data) {
        NetWorthCalculator.Report report = data.report();
        List<String[]> rows = new ArrayList<>();

        rows.add(
                new String[] {
                    "Mês", "Contas", "Investimentos", "Dívidas", "Patrimônio líquido", "Variação"
                });
        BigDecimal previous = null;
        for (NetWorthCalculator.Point point : report.history()) {
            rows.add(
                    new String[] {
                        point.month().toString(),
                        point.cash().toPlainString(),
                        point.investments().toPlainString(),
                        point.debts().toPlainString(),
                        point.netWorth().toPlainString(),
                        previous == null ? "" : point.netWorth().subtract(previous).toPlainString()
                    });
            previous = point.netWorth();
        }

        rows.add(new String[] {});
        rows.add(new String[] {"Conta", "Moeda", "Saldo na moeda", "Saldo em reais"});
        for (NetWorthCalculator.AccountRow row : report.accounts()) {
            rows.add(
                    new String[] {
                        row.account().name(),
                        row.account().currency().name(),
                        row.balance().toPlainString(),
                        row.balanceInBrl().toPlainString()
                    });
        }

        rows.add(new String[] {});
        rows.add(
                new String[] {
                    "Investimento",
                    "Moeda",
                    "Aplicado",
                    "Valor atual",
                    "Rendimento",
                    "Rendimento (%)",
                    "Valor atual em reais",
                    "Rendimento em reais",
                    "Valor informado em"
                });
        for (NetWorthCalculator.InvestmentRow row : report.investments()) {
            rows.add(
                    new String[] {
                        row.account().name(),
                        row.account().currency().name(),
                        row.invested().toPlainString(),
                        row.currentValue().toPlainString(),
                        row.gain().toPlainString(),
                        row.gainRate() == null
                                ? ""
                                : row.gainRate()
                                        .movePointRight(2)
                                        .setScale(2, RoundingMode.HALF_UP)
                                        .toPlainString(),
                        row.currentValueInBrl().toPlainString(),
                        row.gainInBrl().toPlainString(),
                        row.lastValuation() == null
                                ? ""
                                : row.lastValuation().valuationDate().format(DATE_FORMAT)
                    });
        }

        rows.add(new String[] {});
        rows.add(new String[] {"Dívida", "Tipo", "Credor", "Saldo devedor", "Saldo informado em"});
        for (NetWorthCalculator.DebtRow row : report.debts()) {
            rows.add(
                    new String[] {
                        row.debt().name(),
                        debtTypeLabel(row.debt().type()),
                        row.debt().creditor(),
                        row.currentBalance().toPlainString(),
                        row.lastBalance() == null
                                ? ""
                                : row.lastBalance().balanceDate().format(DATE_FORMAT)
                    });
        }
        return CsvWriter.write(rows);
    }

    private String debtTypeLabel(DebtType type) {
        return switch (type) {
            case FINANCING -> "Financiamento";
            case LOAN -> "Empréstimo";
            case CREDIT_CARD -> "Cartão de crédito";
            case OTHER -> "Outra";
        };
    }

    private String foreignCurrency(Money foreignValue) {
        return foreignValue == null ? "" : foreignValue.currency().name();
    }

    private String foreignAmount(Money foreignValue) {
        return foreignValue == null ? "" : foreignValue.amount().toPlainString();
    }

    private String capitalizeMonth(Month month) {
        String name = month.getDisplayName(TextStyle.FULL, PT_BR);
        return name.substring(0, 1).toUpperCase(PT_BR) + name.substring(1);
    }
}
