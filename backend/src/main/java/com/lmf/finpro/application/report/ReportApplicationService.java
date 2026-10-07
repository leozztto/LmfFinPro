package com.lmf.finpro.application.report;

import com.lmf.finpro.domain.model.ReportFormat;
import com.lmf.finpro.domain.model.ReportGranularity;
import com.lmf.finpro.domain.port.out.ReceiptGeneratorPort;
import com.lmf.finpro.domain.port.out.ReportCsvExporterPort;
import java.time.Year;
import java.time.YearMonth;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Porta de entrada dos relatórios com PDF/CSV: escolhe o formato de saída e delega a montagem dos
 * dados de cada tipo de relatório ao seu próprio montador ({@code *DataFactory}), onde ficam as
 * regras de cada um.
 */
@Slf4j
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
            Long currentHouseholdId,
            Long currentUserId,
            Long clientId,
            YearMonth referenceMonth,
            ReportFormat format) {
        log.debug(
                "Gerando recibo do cliente={} mês={} formato={} para o usuário={}",
                clientId,
                referenceMonth,
                format,
                currentHouseholdId);
        var data =
                clientReportDataFactory.buildReceipt(
                        currentHouseholdId, currentUserId, clientId, referenceMonth);
        return format == ReportFormat.CSV
                ? reportCsvExporterPort.exportClientReceipt(data)
                : receiptGeneratorPort.generateClientReceipt(data);
    }

    public byte[] generateAccountStatement(
            Long currentHouseholdId,
            Long currentUserId,
            Long accountId,
            YearMonth referenceMonth,
            ReportFormat format) {
        log.debug(
                "Gerando extrato da conta={} mês={} formato={} para o usuário={}",
                accountId,
                referenceMonth,
                format,
                currentHouseholdId);
        var data =
                accountStatementDataFactory.build(
                        currentHouseholdId, currentUserId, accountId, referenceMonth);
        return format == ReportFormat.CSV
                ? reportCsvExporterPort.exportAccountStatement(data)
                : receiptGeneratorPort.generateAccountStatement(data);
    }

    public byte[] generateClientAnnualStatement(
            Long currentHouseholdId,
            Long currentUserId,
            Long clientId,
            Year year,
            ReportFormat format) {
        log.debug(
                "Gerando extrato anual do cliente={} ano={} formato={} para o usuário={}",
                clientId,
                year,
                format,
                currentHouseholdId);
        var data =
                clientReportDataFactory.buildAnnualStatement(
                        currentHouseholdId, currentUserId, clientId, year);
        return format == ReportFormat.CSV
                ? reportCsvExporterPort.exportClientAnnualStatement(data)
                : receiptGeneratorPort.generateClientAnnualStatement(data);
    }

    public byte[] generateCategoryExpenseReport(
            Long currentHouseholdId,
            Long currentUserId,
            YearMonth referenceMonth,
            ReportFormat format) {
        log.debug(
                "Gerando relatório de despesas por categoria mês={} formato={} para o usuário={}",
                referenceMonth,
                format,
                currentHouseholdId);
        var data =
                categoryExpenseDataFactory.build(currentHouseholdId, currentUserId, referenceMonth);
        return format == ReportFormat.CSV
                ? reportCsvExporterPort.exportCategoryExpenseReport(data)
                : receiptGeneratorPort.generateCategoryExpenseReport(data);
    }

    public byte[] generateIncomeStatement(
            Long currentHouseholdId,
            Long currentUserId,
            Year year,
            ReportGranularity granularity,
            ReportFormat format) {
        log.debug(
                "Gerando DRE ano={} granularidade={} formato={} para o usuário={}",
                year,
                granularity,
                format,
                currentHouseholdId);
        var data =
                incomeStatementDataFactory.build(
                        currentHouseholdId, currentUserId, year, granularity);
        return format == ReportFormat.CSV
                ? reportCsvExporterPort.exportIncomeStatement(data)
                : receiptGeneratorPort.generateIncomeStatement(data);
    }

    public byte[] generateBudgetVsActualReport(
            Long currentHouseholdId,
            Long currentUserId,
            YearMonth referenceMonth,
            ReportFormat format) {
        log.debug(
                "Gerando relatório de orçado x realizado mês={} formato={} para o usuário={}",
                referenceMonth,
                format,
                currentHouseholdId);
        var data =
                budgetVsActualDataFactory.build(currentHouseholdId, currentUserId, referenceMonth);
        return format == ReportFormat.CSV
                ? reportCsvExporterPort.exportBudgetVsActualReport(data)
                : receiptGeneratorPort.generateBudgetVsActualReport(data);
    }

    public byte[] generateTransactionExport(
            Long currentHouseholdId, YearMonth referenceMonth, ReportFormat format) {
        log.debug(
                "Exportando transações mês={} formato={} para o usuário={}",
                referenceMonth,
                format,
                currentHouseholdId);
        var data = transactionExportDataFactory.build(currentHouseholdId, referenceMonth);
        return format == ReportFormat.PDF
                ? receiptGeneratorPort.generateTransactionExport(data)
                : reportCsvExporterPort.exportTransactions(data);
    }
}
