package com.lmf.finpro.application.report;

import com.lmf.finpro.application.networth.NetWorthApplicationService;
import com.lmf.finpro.domain.exception.ResourceNotFoundException;
import com.lmf.finpro.domain.model.NetWorthReportData;
import com.lmf.finpro.domain.model.ReportFormat;
import com.lmf.finpro.domain.model.User;
import com.lmf.finpro.domain.port.out.ReceiptGeneratorPort;
import com.lmf.finpro.domain.port.out.ReportCsvExporterPort;
import com.lmf.finpro.domain.port.out.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Relatório de evolução patrimonial: o patrimônio líquido (contas + investimentos − dívidas) mês a
 * mês e a composição de hoje. Reaproveita o cálculo da tela de Patrimônio, então os valores do PDF
 * e do CSV sempre batem com o que o usuário vê na tela.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NetWorthReportApplicationService {

    private final NetWorthApplicationService netWorthApplicationService;
    private final UserRepositoryPort userRepositoryPort;
    private final ReceiptGeneratorPort receiptGeneratorPort;
    private final ReportCsvExporterPort reportCsvExporterPort;

    public byte[] generate(Long currentUserId, int months, ReportFormat format) {
        log.debug(
                "Gerando relatório de patrimônio meses={} formato={} para o usuário={}",
                months,
                format,
                currentUserId);
        NetWorthReportData data = buildData(currentUserId, months);
        return format == ReportFormat.CSV
                ? reportCsvExporterPort.exportNetWorthReport(data)
                : receiptGeneratorPort.generateNetWorthReport(data);
    }

    NetWorthReportData buildData(Long currentUserId, int months) {
        User issuer =
                userRepositoryPort
                        .findById(currentUserId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Usuário não encontrado: " + currentUserId));
        return new NetWorthReportData(
                issuer, months, netWorthApplicationService.summary(currentUserId, months));
    }
}
