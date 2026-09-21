package com.lmf.finpro.integration.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.lmf.finpro.domain.model.DocumentType;
import com.lmf.finpro.domain.model.TaxRegime;
import com.lmf.finpro.infrastructure.web.dto.auth.AuthResponse;
import com.lmf.finpro.infrastructure.web.dto.auth.LoginRequest;
import com.lmf.finpro.infrastructure.web.dto.auth.RegisterRequest;
import com.lmf.finpro.infrastructure.web.exception.ApiError;
import com.lmf.finpro.integration.support.AbstractIntegrationTest;
import com.lmf.finpro.integration.support.CnpjTestFactory;
import com.lmf.finpro.integration.support.CpfTestFactory;
import com.lmf.finpro.integration.support.TestDataFactory;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class AuthIntegrationTest extends AbstractIntegrationTest {

    @Test
    void registerCreatesUserAndReturnsToken() {
        RegisterRequest request =
                new RegisterRequest(
                        "Ana Freelancer",
                        "ana-" + UUID.randomUUID() + "@finpro.test",
                        "senha12345",
                        DocumentType.CPF,
                        CpfTestFactory.randomValidCpf(),
                        "11987654321",
                        TaxRegime.AUTONOMO,
                        TestDataFactory.sampleAddress());

        ResponseEntity<AuthResponse> response =
                restTemplate.postForEntity("/api/auth/register", request, AuthResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().token()).isNotBlank();
        assertThat(response.getBody().email()).isEqualTo(request.email());
    }

    @Test
    void registerWithCnpjCreatesUserAndReturnsToken() {
        RegisterRequest request =
                new RegisterRequest(
                        "Ana Consultoria",
                        "cnpj-" + UUID.randomUUID() + "@finpro.test",
                        "senha12345",
                        DocumentType.CNPJ,
                        CnpjTestFactory.randomValidCnpj(),
                        null,
                        TaxRegime.MEI,
                        TestDataFactory.sampleAddress());

        ResponseEntity<AuthResponse> response =
                restTemplate.postForEntity("/api/auth/register", request, AuthResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void registerWithDuplicateEmailReturnsConflict() {
        String email = "dup-" + UUID.randomUUID() + "@finpro.test";
        RegisterRequest request =
                new RegisterRequest(
                        "Ana Freelancer",
                        email,
                        "senha12345",
                        DocumentType.CPF,
                        CpfTestFactory.randomValidCpf(),
                        null,
                        TaxRegime.AUTONOMO,
                        TestDataFactory.sampleAddress());
        restTemplate.postForEntity("/api/auth/register", request, AuthResponse.class);

        ResponseEntity<ApiError> response =
                restTemplate.postForEntity("/api/auth/register", request, ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void registerWithInvalidCpfReturnsBadRequest() {
        RegisterRequest request =
                new RegisterRequest(
                        "Ana Freelancer",
                        "invalidcpf-" + UUID.randomUUID() + "@finpro.test",
                        "senha12345",
                        DocumentType.CPF,
                        "12345678900",
                        null,
                        TaxRegime.AUTONOMO,
                        TestDataFactory.sampleAddress());

        ResponseEntity<ApiError> response =
                restTemplate.postForEntity("/api/auth/register", request, ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void registerWithCpfNumberButCnpjTypeReturnsBadRequest() {
        RegisterRequest request =
                new RegisterRequest(
                        "Ana Freelancer",
                        "mismatched-" + UUID.randomUUID() + "@finpro.test",
                        "senha12345",
                        DocumentType.CNPJ,
                        CpfTestFactory.randomValidCpf(),
                        null,
                        TaxRegime.MEI,
                        TestDataFactory.sampleAddress());

        ResponseEntity<ApiError> response =
                restTemplate.postForEntity("/api/auth/register", request, ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void registerWithTaxRegimeMismatchedToDocumentTypeReturnsBadRequest() {
        RegisterRequest request =
                new RegisterRequest(
                        "Ana Freelancer",
                        "regime-mismatch-" + UUID.randomUUID() + "@finpro.test",
                        "senha12345",
                        DocumentType.CPF,
                        CpfTestFactory.randomValidCpf(),
                        null,
                        TaxRegime.MEI,
                        TestDataFactory.sampleAddress());

        ResponseEntity<ApiError> response =
                restTemplate.postForEntity("/api/auth/register", request, ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void registerWithDuplicateDocumentReturnsConflict() {
        String cpf = CpfTestFactory.randomValidCpf();
        restTemplate.postForEntity(
                "/api/auth/register",
                new RegisterRequest(
                        "Ana Freelancer",
                        "doc1-" + UUID.randomUUID() + "@finpro.test",
                        "senha12345",
                        DocumentType.CPF,
                        cpf,
                        null,
                        TaxRegime.AUTONOMO,
                        TestDataFactory.sampleAddress()),
                AuthResponse.class);

        ResponseEntity<ApiError> response =
                restTemplate.postForEntity(
                        "/api/auth/register",
                        new RegisterRequest(
                                "Ana Freelancer",
                                "doc2-" + UUID.randomUUID() + "@finpro.test",
                                "senha12345",
                                DocumentType.CPF,
                                cpf,
                                null,
                                TaxRegime.AUTONOMO,
                                TestDataFactory.sampleAddress()),
                        ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void loginWithValidCredentialsReturnsToken() {
        String email = "login-" + UUID.randomUUID() + "@finpro.test";
        restTemplate.postForEntity(
                "/api/auth/register",
                new RegisterRequest(
                        "Ana Freelancer",
                        email,
                        "senha12345",
                        DocumentType.CPF,
                        CpfTestFactory.randomValidCpf(),
                        null,
                        TaxRegime.AUTONOMO,
                        TestDataFactory.sampleAddress()),
                AuthResponse.class);

        ResponseEntity<AuthResponse> response =
                restTemplate.postForEntity(
                        "/api/auth/login",
                        new LoginRequest(email, "senha12345"),
                        AuthResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().token()).isNotBlank();
    }

    @Test
    void loginWithWrongPasswordReturnsUnauthorized() {
        String email = "wrongpass-" + UUID.randomUUID() + "@finpro.test";
        restTemplate.postForEntity(
                "/api/auth/register",
                new RegisterRequest(
                        "Ana Freelancer",
                        email,
                        "senha12345",
                        DocumentType.CPF,
                        CpfTestFactory.randomValidCpf(),
                        null,
                        TaxRegime.AUTONOMO,
                        TestDataFactory.sampleAddress()),
                AuthResponse.class);

        ResponseEntity<ApiError> response =
                restTemplate.postForEntity(
                        "/api/auth/login", new LoginRequest(email, "senha-errada"), ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void protectedRouteWithoutTokenReturnsUnauthorized() {
        ResponseEntity<ApiError> response =
                restTemplate.exchange(
                        "/api/accounts",
                        HttpMethod.GET,
                        new HttpEntity<>(new HttpHeaders()),
                        ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void protectedRouteWithMalformedAuthorizationHeaderReturnsUnauthorized() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, "NotBearer algum-valor");

        ResponseEntity<ApiError> response =
                restTemplate.exchange(
                        "/api/accounts", HttpMethod.GET, new HttpEntity<>(headers), ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void protectedRouteWithInvalidTokenReturnsUnauthorized() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer token-invalido-e-mal-formado");

        ResponseEntity<ApiError> response =
                restTemplate.exchange(
                        "/api/accounts", HttpMethod.GET, new HttpEntity<>(headers), ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
