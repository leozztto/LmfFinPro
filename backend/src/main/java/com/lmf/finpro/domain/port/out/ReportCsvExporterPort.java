package com.lmf.finpro.domain.port.out;

import com.lmf.finpro.domain.model.AccountStatementData;
import com.lmf.finpro.domain.model.BudgetVsActualReportData;
import com.lmf.finpro.domain.model.CategoryExpenseReportData;
import com.lmf.finpro.domain.model.ClientAnnualStatementData;
import com.lmf.finpro.domain.model.ClientReceiptData;
import com.lmf.finpro.domain.model.IncomeStatementData;
import com.lmf.finpro.domain.model.TransactionExportData;

public interface ReportCsvExporterPort {
    byte[] exportClientReceipt(ClientReceiptData data);

    byte[] exportAccountStatement(AccountStatementData data);

    byte[] exportClientAnnualStatement(ClientAnnualStatementData data);

    byte[] exportCategoryExpenseReport(CategoryExpenseReportData data);

    byte[] exportIncomeStatement(IncomeStatementData data);

    byte[] exportBudgetVsActualReport(BudgetVsActualReportData data);

    byte[] exportTransactions(TransactionExportData data);
}
