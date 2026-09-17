package com.lmf.finpro.infrastructure.client.viacep;

import com.lmf.finpro.domain.exception.CepNotFoundException;
import com.lmf.finpro.domain.exception.CepServiceUnavailableException;
import com.lmf.finpro.domain.model.BrazilianState;
import com.lmf.finpro.domain.model.CepAddress;
import com.lmf.finpro.domain.port.out.CepLookupPort;
import com.lmf.finpro.infrastructure.config.ViaCepProperties;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.time.Duration;

@Component
public class ViaCepAdapter implements CepLookupPort {

    private final RestClient restClient;

    public ViaCepAdapter(RestClient.Builder restClientBuilder, ViaCepProperties viaCepProperties) {
        HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(viaCepProperties.timeoutMs()))
            .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(viaCepProperties.timeoutMs()));

        this.restClient = restClientBuilder
            .baseUrl(viaCepProperties.baseUrl())
            .requestFactory(requestFactory)
            .build();
    }

    @Override
    public CepAddress lookup(String zipCode) {
        ViaCepResponseDto response;
        try {
            response = restClient.get()
                .uri("/{zipCode}/json/", zipCode)
                .retrieve()
                .body(ViaCepResponseDto.class);
        } catch (RestClientException ex) {
            throw new CepServiceUnavailableException("Não foi possível consultar o CEP no momento", ex);
        }

        if (response == null || Boolean.TRUE.equals(response.erro())) {
            throw new CepNotFoundException("CEP não encontrado: " + zipCode);
        }

        return new CepAddress(
            zipCode,
            response.logradouro(),
            response.complemento(),
            response.bairro(),
            response.localidade(),
            parseState(response.uf())
        );
    }

    private BrazilianState parseState(String uf) {
        try {
            return BrazilianState.valueOf(uf);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new CepNotFoundException("CEP retornou UF inválida: " + uf);
        }
    }
}
