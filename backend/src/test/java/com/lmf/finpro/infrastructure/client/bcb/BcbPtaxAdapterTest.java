package com.lmf.finpro.infrastructure.client.bcb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lmf.finpro.domain.exception.ExchangeRateUnavailableException;
import com.lmf.finpro.domain.model.Currency;
import com.lmf.finpro.domain.model.ExchangeRate;
import com.lmf.finpro.infrastructure.config.BcbPtaxProperties;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

/** Servidor HTTP local simulando a API OData da PTAX, como no teste do ViaCEP. */
class BcbPtaxAdapterTest {

    private static final String PATH =
            "/CotacaoMoedaPeriodo(moeda=@moeda,dataInicial=@dataInicial,dataFinalCotacao=@dataFinalCotacao)";

    private HttpServer server;
    private BcbPtaxAdapter adapter;
    private final AtomicReference<String> lastQuery = new AtomicReference<>();

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.start();
        BcbPtaxProperties properties =
                new BcbPtaxProperties("http://localhost:" + server.getAddress().getPort(), 2000);
        adapter = new BcbPtaxAdapter(RestClient.builder(), properties);
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    private void respondWith(int status, String jsonBody) {
        server.createContext(
                "/",
                exchange -> {
                    lastQuery.set(
                            exchange.getRequestURI().getRawPath()
                                    + "?"
                                    + exchange.getRequestURI().getRawQuery());
                    byte[] bytes = jsonBody.getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().add("Content-Type", "application/json");
                    exchange.sendResponseHeaders(status, bytes.length);
                    exchange.getResponseBody().write(bytes);
                    exchange.close();
                });
    }

    @Test
    void keepsOnlyTheClosingBulletinOfEachDay() {
        respondWith(
                200,
                """
                {"value":[
                  {"cotacaoVenda":5.14470,"dataHoraCotacao":"2026-09-18 10:02:17.05066","tipoBoletim":"Abertura"},
                  {"cotacaoVenda":5.15750,"dataHoraCotacao":"2026-09-18 13:03:34.742036","tipoBoletim":"Fechamento"},
                  {"cotacaoVenda":5.11110,"dataHoraCotacao":"2026-09-21 10:08:10.246126","tipoBoletim":"Abertura"},
                  {"cotacaoVenda":5.12000,"dataHoraCotacao":"2026-09-21 13:05:00.000000","tipoBoletim":"Fechamento"}
                ]}
                """);

        List<ExchangeRate> rates =
                adapter.fetch(Currency.USD, LocalDate.of(2026, 9, 18), LocalDate.of(2026, 9, 21));

        assertThat(rates)
                .containsExactly(
                        new ExchangeRate(
                                Currency.USD, LocalDate.of(2026, 9, 18), new BigDecimal("5.15750")),
                        new ExchangeRate(
                                Currency.USD,
                                LocalDate.of(2026, 9, 21),
                                new BigDecimal("5.12000")));
        assertThat(lastQuery.get())
                .startsWith(PATH)
                .contains("@moeda=%27USD%27")
                .contains("@dataInicial=%2709-18-2026%27")
                .contains("@dataFinalCotacao=%2709-21-2026%27");
    }

    @Test
    void noQuotesInThePeriodReturnsAnEmptyList() {
        respondWith(200, "{\"value\":[]}");

        assertThat(
                        adapter.fetch(
                                Currency.EUR, LocalDate.of(2026, 9, 19), LocalDate.of(2026, 9, 20)))
                .isEmpty();
    }

    @Test
    void serverErrorBecomesUnavailable() {
        respondWith(500, "{}");

        assertThatThrownBy(
                        () ->
                                adapter.fetch(
                                        Currency.USD,
                                        LocalDate.of(2026, 9, 18),
                                        LocalDate.of(2026, 9, 21)))
                .isInstanceOf(ExchangeRateUnavailableException.class);
    }
}
