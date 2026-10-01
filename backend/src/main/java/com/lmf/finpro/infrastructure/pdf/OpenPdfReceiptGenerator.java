package com.lmf.finpro.infrastructure.pdf;

import com.lmf.finpro.domain.model.AccountStatementData;
import com.lmf.finpro.domain.model.BudgetVsActualReportData;
import com.lmf.finpro.domain.model.CategoryExpenseReportData;
import com.lmf.finpro.domain.model.ClientAnnualStatementData;
import com.lmf.finpro.domain.model.ClientReceiptData;
import com.lmf.finpro.domain.model.IncomeStatementData;
import com.lmf.finpro.domain.model.NetWorthReportData;
import com.lmf.finpro.domain.model.TagTotalsReportData;
import com.lmf.finpro.domain.model.TransactionExportData;
import com.lmf.finpro.domain.model.TransactionReportData;
import com.lmf.finpro.domain.port.out.ReceiptGeneratorPort;
import java.time.Clock;
import org.springframework.stereotype.Component;

/**
 * Monta os PDFs com a biblioteca OpenPDF, seguindo a mesma filosofia "sem framework de template" já
 * usada no resto do backend (ex.: {@code CsvTransactionParser} monta CSV na mão).
 *
 * <p>O layout imita os formulários de recibo impressos (estilo RPA): uma única grade contínua com
 * cabeçalhos em azul-claro, formada por tabelas de largura total empilhadas sem espaço entre si.
 * Esta classe só delega cada relatório ao gerador do seu tipo; o layout compartilhado está em
 * {@link PdfReportSupport}.
 */
@Component
public class OpenPdfReceiptGenerator implements ReceiptGeneratorPort {

    private final ClientDocumentsPdf clientDocuments;
    private final AccountStatementPdf accountStatement;
    private final PeriodReportsPdf periodReports;
    private final TransactionListsPdf transactionLists;
    private final NetWorthPdf netWorth;

    /** Relógio de São Paulo: a data de emissão e o rodapé não podem sair no fuso do servidor. */
    public OpenPdfReceiptGenerator(Clock clock) {
        this.clientDocuments = new ClientDocumentsPdf(clock);
        this.accountStatement = new AccountStatementPdf(clock);
        this.periodReports = new PeriodReportsPdf(clock);
        this.transactionLists = new TransactionListsPdf(clock);
        this.netWorth = new NetWorthPdf(clock);
    }

    @Override
    public byte[] generateClientReceipt(ClientReceiptData data) {
        return clientDocuments.generateReceipt(data);
    }

    @Override
    public byte[] generateAccountStatement(AccountStatementData data) {
        return accountStatement.generate(data);
    }

    @Override
    public byte[] generateClientAnnualStatement(ClientAnnualStatementData data) {
        return clientDocuments.generateAnnualStatement(data);
    }

    @Override
    public byte[] generateCategoryExpenseReport(CategoryExpenseReportData data) {
        return periodReports.generateCategoryExpense(data);
    }

    @Override
    public byte[] generateIncomeStatement(IncomeStatementData data) {
        return periodReports.generateIncomeStatement(data);
    }

    @Override
    public byte[] generateBudgetVsActualReport(BudgetVsActualReportData data) {
        return periodReports.generateBudgetVsActual(data);
    }

    @Override
    public byte[] generateTransactionExport(TransactionExportData data) {
        return transactionLists.generateExport(data);
    }

    @Override
    public byte[] generateTransactionReport(TransactionReportData data) {
        return transactionLists.generateTransactionReport(data);
    }

    @Override
    public byte[] generateTagTotalsReport(TagTotalsReportData data) {
        return transactionLists.generateTagTotals(data);
    }

    @Override
    public byte[] generateNetWorthReport(NetWorthReportData data) {
        return netWorth.generate(data);
    }
}
