package com.lmf.finpro.integration.support;

import com.lmf.finpro.domain.model.BrazilianState;
import com.lmf.finpro.domain.model.DocumentType;
import com.lmf.finpro.infrastructure.web.dto.auth.AddressRequest;
import com.lmf.finpro.infrastructure.web.dto.auth.AuthResponse;
import com.lmf.finpro.infrastructure.web.dto.auth.RegisterRequest;
import org.springframework.boot.test.web.client.TestRestTemplate;

import java.util.UUID;

/** Cria um usuário único (via /api/auth/register) e devolve o token JWT pronto para uso nos testes. */
public class TestDataFactory {

    public static TestUser registerRandomUser(TestRestTemplate restTemplate) {
        String email = "user-" + UUID.randomUUID() + "@finpro.test";
        RegisterRequest request = new RegisterRequest(
            "Usuário Teste", email, "senha12345", DocumentType.CPF, CpfTestFactory.randomValidCpf(),
            "11987654321", "SIMPLES_NACIONAL", sampleAddress()
        );

        AuthResponse response = restTemplate.postForObject("/api/auth/register", request, AuthResponse.class);

        return new TestUser(response.userId(), response.email(), response.token());
    }

    public static AddressRequest sampleAddress() {
        return new AddressRequest(
            "01310100", "Avenida Paulista", "1000", "Sala 1", "Bela Vista", "São Paulo", BrazilianState.SP
        );
    }
}
