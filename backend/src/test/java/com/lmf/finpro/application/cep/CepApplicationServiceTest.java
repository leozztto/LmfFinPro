package com.lmf.finpro.application.cep;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.lmf.finpro.domain.model.BrazilianState;
import com.lmf.finpro.domain.model.CepAddress;
import com.lmf.finpro.domain.port.out.CepLookupPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CepApplicationServiceTest {

    @Mock private CepLookupPort cepLookupPort;

    @InjectMocks private CepApplicationService service;

    @Test
    void lookupStripsFormattingBeforeDelegatingToPort() {
        CepAddress address =
                new CepAddress(
                        "01310100",
                        "Avenida Paulista",
                        null,
                        "Bela Vista",
                        "São Paulo",
                        BrazilianState.SP);
        when(cepLookupPort.lookup("01310100")).thenReturn(address);

        assertThat(service.lookup("01310-100")).isEqualTo(address);
    }

    @Test
    void lookupRejectsZipCodeWithLessThanEightDigits() {
        assertThatThrownBy(() -> service.lookup("1234"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void lookupRejectsNullZipCode() {
        assertThatThrownBy(() -> service.lookup(null)).isInstanceOf(IllegalArgumentException.class);
    }
}
