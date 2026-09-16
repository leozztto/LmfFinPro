package com.lmf.finpro.integration.support;

import com.lmf.finpro.infrastructure.web.dto.auth.AuthResponse;
import com.lmf.finpro.infrastructure.web.dto.auth.RegisterRequest;
import org.springframework.boot.test.web.client.TestRestTemplate;

import java.util.UUID;

/** Cria um usuário único (via /api/auth/register) e devolve o token JWT pronto para uso nos testes. */
public class TestDataFactory {

    public static TestUser registerRandomUser(TestRestTemplate restTemplate) {
        String email = "user-" + UUID.randomUUID() + "@finpro.test";
        RegisterRequest request = new RegisterRequest("Usuário Teste", email, "senha12345", "SIMPLES_NACIONAL");

        AuthResponse response = restTemplate.postForObject("/api/auth/register", request, AuthResponse.class);

        return new TestUser(response.userId(), response.email(), response.token());
    }
}
