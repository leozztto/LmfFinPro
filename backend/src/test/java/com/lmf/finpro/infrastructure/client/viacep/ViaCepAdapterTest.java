package com.lmf.finpro.infrastructure.client.viacep;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lmf.finpro.domain.exception.CepNotFoundException;
import com.lmf.finpro.domain.exception.CepServiceUnavailableException;
import com.lmf.finpro.domain.model.BrazilianState;
import com.lmf.finpro.domain.model.CepAddress;
import com.lmf.finpro.infrastructure.config.ViaCepProperties;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

/**
 * Sobe um servidor HTTP local (JDK puro, sem dependência nova) simulando o ViaCEP, para testar o
 * adapter de verdade — parsing de JSON, montagem de URL, tratamento de erro — sem depender da API
 * externa real.
 */
class ViaCepAdapterTest {

    private HttpServer server;
    private ViaCepAdapter adapter;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.start();
        ViaCepProperties properties =
                new ViaCepProperties("http://localhost:" + server.getAddress().getPort(), 2000);
        adapter = new ViaCepAdapter(RestClient.builder(), properties);
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    private void respondWith(String path, int status, String jsonBody) {
        server.createContext(
                path,
                exchange -> {
                    byte[] bytes = jsonBody.getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().add("Content-Type", "application/json");
                    exchange.sendResponseHeaders(status, bytes.length);
                    exchange.getResponseBody().write(bytes);
                    exchange.close();
                });
    }

    @Test
    void looksUpAddressSuccessfully() {
        respondWith(
                "/01310100/json/",
                200,
                """
                {"cep":"01310-100","logradouro":"Avenida Paulista","complemento":"lado ímpar",
                 "bairro":"Bela Vista","localidade":"São Paulo","uf":"SP","erro":null}
                """);

        CepAddress address = adapter.lookup("01310100");

        assertThat(address.street()).isEqualTo("Avenida Paulista");
        assertThat(address.complement()).isEqualTo("lado ímpar");
        assertThat(address.neighborhood()).isEqualTo("Bela Vista");
        assertThat(address.city()).isEqualTo("São Paulo");
        assertThat(address.state()).isEqualTo(BrazilianState.SP);
    }

    @Test
    void throwsCepNotFoundWhenViaCepReportsErro() {
        respondWith("/00000000/json/", 200, "{\"erro\":true}");

        assertThatThrownBy(() -> adapter.lookup("00000000"))
                .isInstanceOf(CepNotFoundException.class);
    }

    @Test
    void throwsCepNotFoundWhenResponseBodyIsEmpty() {
        respondWith("/00000001/json/", 200, "null");

        assertThatThrownBy(() -> adapter.lookup("00000001"))
                .isInstanceOf(CepNotFoundException.class);
    }

    @Test
    void throwsCepNotFoundWhenStateIsNotAValidBrazilianState() {
        respondWith(
                "/01310101/json/",
                200,
                """
                {"cep":"01310-101","logradouro":"Rua X","complemento":"","bairro":"Bairro",
                 "localidade":"Cidade","uf":"ZZ","erro":null}
                """);

        assertThatThrownBy(() -> adapter.lookup("01310101"))
                .isInstanceOf(CepNotFoundException.class);
    }

    @Test
    void throwsCepServiceUnavailableWhenServerReturnsAnErrorStatus() {
        respondWith("/99999999/json/", 500, "{}");

        assertThatThrownBy(() -> adapter.lookup("99999999"))
                .isInstanceOf(CepServiceUnavailableException.class);
    }

    @Test
    void throwsCepServiceUnavailableWhenConnectionFails() {
        server.stop(0); // servidor indisponível antes mesmo da chamada

        assertThatThrownBy(() -> adapter.lookup("01310100"))
                .isInstanceOf(CepServiceUnavailableException.class);
    }
}
