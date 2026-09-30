package com.lmf.finpro.application.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.lmf.finpro.application.networth.NetWorthApplicationService;
import com.lmf.finpro.domain.exception.ResourceNotFoundException;
import com.lmf.finpro.domain.model.Address;
import com.lmf.finpro.domain.model.BrazilianState;
import com.lmf.finpro.domain.model.DocumentType;
import com.lmf.finpro.domain.model.NetWorthCalculator;
import com.lmf.finpro.domain.model.NetWorthReportData;
import com.lmf.finpro.domain.model.NetWorthReportTestData;
import com.lmf.finpro.domain.model.ReportFormat;
import com.lmf.finpro.domain.model.TaxRegime;
import com.lmf.finpro.domain.model.User;
import com.lmf.finpro.domain.port.out.ReceiptGeneratorPort;
import com.lmf.finpro.domain.port.out.ReportCsvExporterPort;
import com.lmf.finpro.domain.port.out.UserRepositoryPort;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NetWorthReportApplicationServiceTest {

    @Mock private NetWorthApplicationService netWorthApplicationService;
    @Mock private UserRepositoryPort userRepositoryPort;
    @Mock private ReceiptGeneratorPort receiptGeneratorPort;
    @Mock private ReportCsvExporterPort reportCsvExporterPort;

    @InjectMocks private NetWorthReportApplicationService service;

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
                "Titular",
                "titular@x.com",
                "hash",
                DocumentType.CPF,
                "11144477735",
                "11999998888",
                TaxRegime.AUTONOMO,
                address,
                LocalDateTime.now(),
                0);
    }

    @Test
    void usesTheSameCalculationAsTheNetWorthScreenAndGeneratesThePdf() {
        User issuer = issuer();
        NetWorthCalculator.Report report = NetWorthReportTestData.report();
        when(userRepositoryPort.findById(10L)).thenReturn(Optional.of(issuer));
        when(netWorthApplicationService.summary(10L, 6)).thenReturn(report);
        when(receiptGeneratorPort.generateNetWorthReport(any())).thenReturn(new byte[] {1});

        byte[] result = service.generate(10L, 6, ReportFormat.PDF);

        assertThat(result).containsExactly(1);
        ArgumentCaptor<NetWorthReportData> captor =
                ArgumentCaptor.forClass(NetWorthReportData.class);
        verify(receiptGeneratorPort).generateNetWorthReport(captor.capture());
        assertThat(captor.getValue().issuer()).isSameAs(issuer);
        assertThat(captor.getValue().months()).isEqualTo(6);
        assertThat(captor.getValue().report()).isSameAs(report);
        verifyNoInteractions(reportCsvExporterPort);
    }

    @Test
    void generatesTheCsvWhenRequested() {
        when(userRepositoryPort.findById(10L)).thenReturn(Optional.of(issuer()));
        when(netWorthApplicationService.summary(10L, 12))
                .thenReturn(NetWorthReportTestData.report());
        when(reportCsvExporterPort.exportNetWorthReport(any())).thenReturn(new byte[] {2});

        byte[] result = service.generate(10L, 12, ReportFormat.CSV);

        assertThat(result).containsExactly(2);
        verify(receiptGeneratorPort, never()).generateNetWorthReport(any());
    }

    @Test
    void throwsWhenTheUserDoesNotExist() {
        when(userRepositoryPort.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generate(10L, 12, ReportFormat.PDF))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(netWorthApplicationService);
    }
}
