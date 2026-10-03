package com.lmf.finpro.infrastructure.client.bcb;

import com.lmf.finpro.domain.exception.ExchangeRateUnavailableException;
import com.lmf.finpro.domain.model.Currency;
import com.lmf.finpro.domain.model.ExchangeRate;
import com.lmf.finpro.domain.port.out.ExchangeRateProviderPort;
import com.lmf.finpro.infrastructure.config.BcbPtaxProperties;
import com.lmf.finpro.infrastructure.logging.SafeErrors;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * PTAX do Banco Central (API OData pública, sem chave). Usa só o boletim de fechamento, pela taxa
 * de venda — a referência oficial para conversão de moeda estrangeira em reais.
 */
@Slf4j
@Component
public class BcbPtaxAdapter implements ExchangeRateProviderPort {

    private static final DateTimeFormatter BCB_DATE = DateTimeFormatter.ofPattern("MM-dd-yyyy");
    private static final String CLOSING_BULLETIN = "Fechamento";

    private final RestClient restClient;
    private final String baseUrl;

    public BcbPtaxAdapter(RestClient.Builder restClientBuilder, BcbPtaxProperties properties) {
        HttpClient httpClient =
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofMillis(properties.timeoutMs()))
                        .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(properties.timeoutMs()));

        this.restClient = restClientBuilder.requestFactory(requestFactory).build();
        this.baseUrl = properties.baseUrl();
    }

    @Override
    public List<ExchangeRate> fetch(Currency currency, LocalDate from, LocalDate to) {
        BcbPtaxResponseDto response;
        try {
            response =
                    restClient
                            .get()
                            .uri(uri(currency, from, to))
                            .retrieve()
                            .body(BcbPtaxResponseDto.class);
        } catch (RestClientException ex) {
            log.warn("Falha na consulta ao Banco Central {}", SafeErrors.describe(ex));
            throw new ExchangeRateUnavailableException(
                    "Não foi possível consultar a cotação do " + currency + " no Banco Central");
        }
        if (response == null || response.value() == null) {
            return List.of();
        }
        return response.value().stream()
                .filter(quote -> CLOSING_BULLETIN.equals(quote.tipoBoletim()))
                .filter(quote -> quote.cotacaoVenda() != null && quote.dataHoraCotacao() != null)
                .map(
                        quote ->
                                new ExchangeRate(
                                        currency,
                                        LocalDate.parse(quote.dataHoraCotacao().substring(0, 10)),
                                        quote.cotacaoVenda()))
                .toList();
    }

    /**
     * Montada à mão: os parâmetros OData ({@code @moeda}, {@code $format}) e as aspas simples não
     * sobrevivem ao template de URI do RestClient.
     */
    private URI uri(Currency currency, LocalDate from, LocalDate to) {
        return URI.create(
                baseUrl
                        + "/CotacaoMoedaPeriodo(moeda=@moeda,dataInicial=@dataInicial,"
                        + "dataFinalCotacao=@dataFinalCotacao)"
                        + "?@moeda=%27"
                        + currency.name()
                        + "%27"
                        + "&@dataInicial=%27"
                        + from.format(BCB_DATE)
                        + "%27"
                        + "&@dataFinalCotacao=%27"
                        + to.format(BCB_DATE)
                        + "%27"
                        + "&$format=json&$select=cotacaoVenda,dataHoraCotacao,tipoBoletim");
    }
}
