package com.lmf.finpro.application.report;

import com.lmf.finpro.domain.model.ReportFormat;
import com.lmf.finpro.domain.model.ReportGranularity;
import com.lmf.finpro.domain.port.out.ReceiptGeneratorPort;
import com.lmf.finpro.domain.port.out.ReportCsvExporterPort;
import java.time.Year;
import java.time.YearMonth;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Porta de entrada dos relatórios com PDF/CSV: escolhe o formato de saída e delega a montagem dos
 * dados de cada tipo de relatório ao seu próprio montador ({@code *DataFactory}), onde ficam as
 * regras de cada um.
 */
@Service
@RequiredArgsConstructor
public class ReportApplicationService {

    private final ClientReportDataFactory clientReportDataFactory;
    private final AccountStatementDataFactory accountStatementDataFactory;
    private final CategoryExpenseDataFactory categoryExpenseDataFactory;
    private final IncomeStatementDataFactory incomeStatementDataFactory;
    private final BudgetVsActualDataFactory budgetVsActualDataFactory;
    private final TransactionExportDataFactory transactionExportDataFactory;
    private final ReceiptGeneratorPort receiptGeneratorPort;
    private final ReportCsvExporterPort reportCsvExporterPort;

    public byte[] generateClientReceipt(
            Long currentUserId, Long clientId, YearMonth referenceMonth, ReportFormat format) {
        var data = clientReportDataFactory.buildReceipt(currentUserId, clientId, referenceMonth);
        return format == ReportFormat.CSV
                ? reportCsvExporterPort.exportClientReceipt(data)
                : receiptGeneratorPort.generateClientReceipt(data);
    }

    public byte[] generateAccountStatement(
            Long currentUserId, Long accountId, YearMonth referenceMonth, ReportFormat format) {
        var data = accountStatementDataFactory.build(currentUserId, accountId, referenceMonth);
        return format == ReportFormat.CSV
                ? reportCsvExporterPort.exportAccountStatement(data)
                : receiptGeneratorPort.generateAccountStatement(data);
    }

    public byte[] generateClientAnnualStatement(
            Long currentUserId, Long clientId, Year year, ReportFormat format) {
        var data = clientReportDataFactory.buildAnnualStatement(currentUserId, clientId, year);
        return format == ReportFormat.CSV
                ? reportCsvExporterPort.exportClientAnnualStatement(data)
                : receiptGeneratorPort.generateClientAnnualStatement(data);
    }

    public byte[] generateCategoryExpenseReport(
            Long currentUserId, YearMonth referenceMonth, ReportFormat format) {
        var data = categoryExpenseDataFactory.build(currentUserId, referenceMonth);
        return format == ReportFormat.CSV
                ? reportCsvExporterPort.exportCategoryExpenseReport(data)
                : receiptGeneratorPort.generateCategoryExpenseReport(data);
    }

    public byte[] generateIncomeStatement(
            Long currentUserId, Year year, ReportGranularity granularity, ReportFormat format) {
        var data = incomeStatementDataFactory.build(currentUserId, year, granularity);
        return format == ReportFormat.CSV
                ? reportCsvExporterPort.exportIncomeStatement(data)
                : receiptGeneratorPort.generateIncomeStatement(data);
    }

    public byte[] generateBudgetVsActualReport(
            Long currentUserId, YearMonth referenceMonth, ReportFormat format) {
        var data = budgetVsActualDataFactory.build(currentUserId, referenceMonth);
        return format == ReportFormat.CSV
                ? reportCsvExporterPort.exportBudgetVsActualReport(data)
                : receiptGeneratorPort.generateBudgetVsActualReport(data);
    }

    public byte[] generateTransactionExport(
            Long currentUserId, YearMonth referenceMonth, ReportFormat format) {
        var data = transactionExportDataFactory.build(currentUserId, referenceMonth);
        return format == ReportFormat.PDF
                ? receiptGeneratorPort.generateTransactionExport(data)
                : reportCsvExporterPort.exportTransactions(data);
    }
}
