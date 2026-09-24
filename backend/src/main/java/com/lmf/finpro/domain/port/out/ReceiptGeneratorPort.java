package com.lmf.finpro.domain.port.out;

import com.lmf.finpro.domain.model.AccountStatementData;
import com.lmf.finpro.domain.model.BudgetVsActualReportData;
import com.lmf.finpro.domain.model.CategoryExpenseReportData;
import com.lmf.finpro.domain.model.ClientAnnualStatementData;
import com.lmf.finpro.domain.model.ClientReceiptData;
import com.lmf.finpro.domain.model.IncomeStatementData;

public interface ReceiptGeneratorPort {
    byte[] generateClientReceipt(ClientReceiptData data);

    byte[] generateAccountStatement(AccountStatementData data);

    byte[] generateClientAnnualStatement(ClientAnnualStatementData data);

    byte[] generateCategoryExpenseReport(CategoryExpenseReportData data);

    byte[] generateIncomeStatement(IncomeStatementData data);

    byte[] generateBudgetVsActualReport(BudgetVsActualReportData data);
}
