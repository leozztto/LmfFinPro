package com.lmf.finpro.integration.support;

import com.lmf.finpro.domain.model.BrazilianState;
import com.lmf.finpro.domain.model.DocumentType;
import com.lmf.finpro.domain.model.TaxRegime;
import com.lmf.finpro.infrastructure.web.dto.auth.AddressRequest;
import com.lmf.finpro.infrastructure.web.dto.auth.AuthResponse;
import com.lmf.finpro.infrastructure.web.dto.auth.RegisterRequest;
import com.lmf.finpro.infrastructure.web.dto.common.PageResponse;
import com.lmf.finpro.infrastructure.web.dto.transaction.TransactionResponse;
import java.util.UUID;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;

/**
 * Cria um usuário único (via /api/auth/register) e devolve o token JWT pronto para uso nos testes.
 */
public class TestDataFactory {

    public static TestUser registerRandomUser(TestRestTemplate restTemplate) {
        String email = "user-" + UUID.randomUUID() + "@finpro.test";
        RegisterRequest request =
                new RegisterRequest(
                        "Usuário Teste",
                        email,
                        "senha12345",
                        DocumentType.CPF,
                        CpfTestFactory.randomValidCpf(),
                        "11987654321",
                        TaxRegime.AUTONOMO,
                        sampleAddress());

        AuthResponse response =
                restTemplate.postForObject("/api/auth/register", request, AuthResponse.class);

        return new TestUser(response.userId(), response.email(), response.token());
    }

    public static AddressRequest sampleAddress() {
        return new AddressRequest(
                "01310100",
                "Avenida Paulista",
                "1000",
                "Sala 1",
                "Bela Vista",
                "São Paulo",
                BrazilianState.SP);
    }

    /** Uma página de {@code GET /api/transactions}; {@code query} é a query string sem o "?". */
    public static PageResponse<TransactionResponse> transactionsPage(
            TestRestTemplate restTemplate, TestUser user, String query) {
        return restTemplate
                .exchange(
                        "/api/transactions" + (query.isEmpty() ? "" : "?" + query),
                        HttpMethod.GET,
                        new HttpEntity<>(user.authHeaders()),
                        new ParameterizedTypeReference<PageResponse<TransactionResponse>>() {})
                .getBody();
    }

    /** Todas as transações do usuário (testes têm poucas): a primeira página, no tamanho máximo. */
    public static java.util.List<TransactionResponse> listTransactions(
            TestRestTemplate restTemplate, TestUser user) {
        return transactionsPage(restTemplate, user, "size=100").content();
    }
}
