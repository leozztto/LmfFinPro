package com.lmf.finpro.domain.port.out;

import com.lmf.finpro.domain.model.AccountStatementData;
import com.lmf.finpro.domain.model.CategoryExpenseReportData;
import com.lmf.finpro.domain.model.ClientAnnualStatementData;
import com.lmf.finpro.domain.model.ClientReceiptData;

public interface ReceiptGeneratorPort {
    byte[] generateClientReceipt(ClientReceiptData data);

    byte[] generateAccountStatement(AccountStatementData data);

    byte[] generateClientAnnualStatement(ClientAnnualStatementData data);

    byte[] generateCategoryExpenseReport(CategoryExpenseReportData data);
}
