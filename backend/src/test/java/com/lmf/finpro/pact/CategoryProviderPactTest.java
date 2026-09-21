package com.lmf.finpro.pact;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import au.com.dius.pact.provider.junitsupport.loader.PactBrokerAuth;
import au.com.dius.pact.provider.spring.junit5.PactVerificationSpringProvider;
import com.lmf.finpro.infrastructure.web.dto.category.CategoryRequest;
import com.lmf.finpro.infrastructure.web.dto.category.CategoryResponse;
import com.lmf.finpro.integration.support.AbstractIntegrationTest;
import com.lmf.finpro.integration.support.TestDataFactory;
import com.lmf.finpro.integration.support.TestUser;
import org.apache.hc.core5.http.HttpRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;

/**
 * Verifica que o backend satisfaz o contrato publicado pelo frontend (consumer) no PactFlow — ver
 * {@code frontend/src/features/categories/api/categoriesApi.pact.test.ts}.
 *
 * <p>Só roda quando {@code PACT_BROKER_TOKEN} está presente (CI); localmente, sem o token, o JUnit
 * pula a classe inteira em vez de falhar o {@code mvn verify} de todo mundo por falta de credencial
 * de um serviço externo — mesmo padrão já usado pro scan do SonarCloud no CI.
 */
@Provider("LmfFinPro-backend")
@PactBroker(
        url = "${pactbroker.url}",
        authentication = @PactBrokerAuth(token = "${pactbroker.token}"))
@EnabledIfEnvironmentVariable(named = "PACT_BROKER_TOKEN", matches = ".+")
class CategoryProviderPactTest extends AbstractIntegrationTest {

    @LocalServerPort private int port;

    private String authToken;

    @BeforeEach
    void setTarget(PactVerificationContext context) {
        context.setTarget(new HttpTestTarget("localhost", port));
    }

    /**
     * O contrato só define um Authorization "de mentira" (qualquer string) — quem coloca um token
     * de verdade é aqui, mutando a requisição antes do Pact disparar contra o backend real. Esse
     * `HttpRequest` extra é injetado pelo próprio {@link PactVerificationSpringProvider} (via
     * JUnit5 {@code ParameterResolver}), não é um parâmetro comum de teste.
     */
    @TestTemplate
    @ExtendWith(PactVerificationSpringProvider.class)
    void pactVerificationTestTemplate(PactVerificationContext context, HttpRequest request) {
        request.removeHeaders("Authorization");
        request.addHeader("Authorization", "Bearer " + authToken);
        context.verifyInteraction();
    }

    @State("o usuário autenticado tem ao menos uma categoria")
    void userHasACategory() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        CategoryRequest categoryRequest =
                new CategoryRequest(
                        "Alimentação",
                        com.lmf.finpro.domain.model.CategoryType.EXPENSE,
                        "#2E6E4E",
                        "utensils");
        restTemplate.exchange(
                "/api/categories",
                HttpMethod.POST,
                new HttpEntity<>(categoryRequest, user.authHeaders()),
                CategoryResponse.class);
        authToken = user.token();
    }
}
