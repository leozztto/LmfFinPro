package com.lmf.finpro.integration.cep;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.lmf.finpro.domain.exception.CepNotFoundException;
import com.lmf.finpro.domain.exception.CepServiceUnavailableException;
import com.lmf.finpro.domain.model.BrazilianState;
import com.lmf.finpro.domain.model.CepAddress;
import com.lmf.finpro.domain.port.out.CepLookupPort;
import com.lmf.finpro.infrastructure.web.dto.cep.CepResponse;
import com.lmf.finpro.infrastructure.web.exception.ApiError;
import com.lmf.finpro.integration.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Cobre o endpoint público de CEP de ponta a ponta (controller + mapper + HTTP real), sem depender
 * da API externa do ViaCEP de verdade: {@link CepLookupPort} é substituído por um mock.
 */
class CepIntegrationTest extends AbstractIntegrationTest {

    @MockBean private CepLookupPort cepLookupPort;

    @Test
    void returnsAddressForAValidZipCode() {
        CepAddress address =
                new CepAddress(
                        "01310100",
                        "Avenida Paulista",
                        "lado ímpar",
                        "Bela Vista",
                        "São Paulo",
                        BrazilianState.SP);
        when(cepLookupPort.lookup("01310100")).thenReturn(address);

        ResponseEntity<CepResponse> response =
                restTemplate.getForEntity("/api/cep/01310100", CepResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().street()).isEqualTo("Avenida Paulista");
        assertThat(response.getBody().state()).isEqualTo(BrazilianState.SP);
    }

    @Test
    void acceptsAFormattedZipCodeAndStripsNonDigitsBeforeLookup() {
        CepAddress address =
                new CepAddress(
                        "01310100",
                        "Avenida Paulista",
                        null,
                        "Bela Vista",
                        "São Paulo",
                        BrazilianState.SP);
        when(cepLookupPort.lookup("01310100")).thenReturn(address);

        ResponseEntity<CepResponse> response =
                restTemplate.getForEntity("/api/cep/01310-100", CepResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void returnsNotFoundWhenZipCodeDoesNotExist() {
        when(cepLookupPort.lookup("00000000"))
                .thenThrow(new CepNotFoundException("CEP não encontrado: 00000000"));

        ResponseEntity<ApiError> response =
                restTemplate.getForEntity("/api/cep/00000000", ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void returnsServiceUnavailableWhenViaCepIsDown() {
        when(cepLookupPort.lookup("01310100"))
                .thenThrow(
                        new CepServiceUnavailableException(
                                "Serviço de CEP indisponível", new RuntimeException("timeout")));

        ResponseEntity<ApiError> response =
                restTemplate.getForEntity("/api/cep/01310100", ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void returnsInternalServerErrorForAnUnexpectedException() {
        when(cepLookupPort.lookup("01310100")).thenThrow(new RuntimeException("falha inesperada"));

        ResponseEntity<ApiError> response =
                restTemplate.getForEntity("/api/cep/01310100", ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().message()).isEqualTo("Erro interno inesperado");
    }

    @Test
    void returnsBadRequestWhenZipCodeHasWrongLength() {
        ResponseEntity<ApiError> response =
                restTemplate.getForEntity("/api/cep/123", ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void doesNotRequireAuthentication() {
        // Nenhum header de Authorization enviado — /api/cep/** é público.
        CepAddress address =
                new CepAddress(
                        "01310100",
                        "Avenida Paulista",
                        null,
                        "Bela Vista",
                        "São Paulo",
                        BrazilianState.SP);
        when(cepLookupPort.lookup("01310100")).thenReturn(address);

        ResponseEntity<CepResponse> response =
                restTemplate.getForEntity("/api/cep/01310100", CepResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
