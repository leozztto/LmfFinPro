package com.lmf.finpro.integration.auth;

import com.lmf.finpro.infrastructure.web.dto.auth.AuthResponse;
import com.lmf.finpro.infrastructure.web.dto.auth.LoginRequest;
import com.lmf.finpro.infrastructure.web.dto.auth.RegisterRequest;
import com.lmf.finpro.infrastructure.web.exception.ApiError;
import com.lmf.finpro.integration.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AuthIntegrationTest extends AbstractIntegrationTest {

    @Test
    void registerCreatesUserAndReturnsToken() {
        RegisterRequest request = new RegisterRequest(
            "Ana Freelancer", "ana-" + UUID.randomUUID() + "@finpro.test", "senha12345", "SIMPLES_NACIONAL"
        );

        ResponseEntity<AuthResponse> response = restTemplate.postForEntity("/api/auth/register", request, AuthResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().token()).isNotBlank();
        assertThat(response.getBody().email()).isEqualTo(request.email());
    }

    @Test
    void registerWithDuplicateEmailReturnsConflict() {
        String email = "dup-" + UUID.randomUUID() + "@finpro.test";
        RegisterRequest request = new RegisterRequest("Ana", email, "senha12345", null);
        restTemplate.postForEntity("/api/auth/register", request, AuthResponse.class);

        ResponseEntity<ApiError> response = restTemplate.postForEntity("/api/auth/register", request, ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void loginWithValidCredentialsReturnsToken() {
        String email = "login-" + UUID.randomUUID() + "@finpro.test";
        restTemplate.postForEntity("/api/auth/register", new RegisterRequest("Ana", email, "senha12345", null), AuthResponse.class);

        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
            "/api/auth/login", new LoginRequest(email, "senha12345"), AuthResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().token()).isNotBlank();
    }

    @Test
    void loginWithWrongPasswordReturnsUnauthorized() {
        String email = "wrongpass-" + UUID.randomUUID() + "@finpro.test";
        restTemplate.postForEntity("/api/auth/register", new RegisterRequest("Ana", email, "senha12345", null), AuthResponse.class);

        ResponseEntity<ApiError> response = restTemplate.postForEntity(
            "/api/auth/login", new LoginRequest(email, "senha-errada"), ApiError.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void protectedRouteWithoutTokenReturnsUnauthorized() {
        ResponseEntity<ApiError> response = restTemplate.exchange(
            "/api/accounts", HttpMethod.GET, new HttpEntity<>(new HttpHeaders()), ApiError.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
