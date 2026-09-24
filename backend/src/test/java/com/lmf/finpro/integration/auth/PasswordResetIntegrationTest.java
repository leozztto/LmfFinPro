package com.lmf.finpro.integration.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.lmf.finpro.domain.port.out.PasswordResetMailerPort;
import com.lmf.finpro.infrastructure.web.dto.auth.AuthResponse;
import com.lmf.finpro.infrastructure.web.dto.auth.ForgotPasswordRequest;
import com.lmf.finpro.infrastructure.web.dto.auth.LoginRequest;
import com.lmf.finpro.infrastructure.web.dto.auth.ResetPasswordRequest;
import com.lmf.finpro.infrastructure.web.exception.ApiError;
import com.lmf.finpro.integration.support.AbstractIntegrationTest;
import com.lmf.finpro.integration.support.TestDataFactory;
import com.lmf.finpro.integration.support.TestUser;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class PasswordResetIntegrationTest extends AbstractIntegrationTest {

    /** Substitui o envio de e-mail por um "caixa de entrada" em memória: e-mail → último link. */
    static class CapturingMailer implements PasswordResetMailerPort {
        final Map<String, String> lastLinkByEmail = new ConcurrentHashMap<>();

        @Override
        public void sendResetLink(String toEmail, String userName, String resetLink, long ttl) {
            lastLinkByEmail.put(toEmail, resetLink);
        }
    }

    @TestConfiguration
    static class MailerConfig {
        @Bean
        @Primary
        CapturingMailer capturingMailer() {
            return new CapturingMailer();
        }
    }

    @Autowired private CapturingMailer mailer;

    private String requestResetAndGetToken(String email) {
        ResponseEntity<Void> response =
                restTemplate.postForEntity(
                        "/api/auth/forgot-password", new ForgotPasswordRequest(email), Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        String link = mailer.lastLinkByEmail.get(email);
        assertThat(link).contains("/redefinir-senha?token=");
        return link.substring(link.indexOf("token=") + "token=".length());
    }

    @Test
    void resetPasswordAllowsLoginWithTheNewPasswordOnly() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        String token = requestResetAndGetToken(user.email());

        ResponseEntity<Void> reset =
                restTemplate.postForEntity(
                        "/api/auth/reset-password",
                        new ResetPasswordRequest(token, "novaSenha123"),
                        Void.class);
        assertThat(reset.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<AuthResponse> newLogin =
                restTemplate.postForEntity(
                        "/api/auth/login",
                        new LoginRequest(user.email(), "novaSenha123"),
                        AuthResponse.class);
        assertThat(newLogin.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<ApiError> oldLogin =
                restTemplate.postForEntity(
                        "/api/auth/login",
                        new LoginRequest(user.email(), "senha12345"),
                        ApiError.class);
        assertThat(oldLogin.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private HttpStatus getAccountsStatus(HttpHeaders headers) {
        return HttpStatus.valueOf(
                restTemplate
                        .exchange(
                                "/api/accounts",
                                HttpMethod.GET,
                                new HttpEntity<>(headers),
                                String.class)
                        .getStatusCode()
                        .value());
    }

    @Test
    void resetPasswordEndsSessionsOpenedBeforeTheChange() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        assertThat(getAccountsStatus(user.authHeaders())).isEqualTo(HttpStatus.OK);

        String token = requestResetAndGetToken(user.email());
        restTemplate.postForEntity(
                "/api/auth/reset-password",
                new ResetPasswordRequest(token, "novaSenha123"),
                Void.class);

        assertThat(getAccountsStatus(user.authHeaders())).isEqualTo(HttpStatus.UNAUTHORIZED);

        AuthResponse newLogin =
                restTemplate.postForObject(
                        "/api/auth/login",
                        new LoginRequest(user.email(), "novaSenha123"),
                        AuthResponse.class);
        TestUser newSession = new TestUser(user.userId(), user.email(), newLogin.token());
        assertThat(getAccountsStatus(newSession.authHeaders())).isEqualTo(HttpStatus.OK);
    }

    @Test
    void tokenCannotBeUsedTwice() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        String token = requestResetAndGetToken(user.email());
        restTemplate.postForEntity(
                "/api/auth/reset-password",
                new ResetPasswordRequest(token, "novaSenha123"),
                Void.class);

        ResponseEntity<ApiError> second =
                restTemplate.postForEntity(
                        "/api/auth/reset-password",
                        new ResetPasswordRequest(token, "outraSenha123"),
                        ApiError.class);

        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void requestingANewLinkInvalidatesThePreviousOne() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        String firstToken = requestResetAndGetToken(user.email());
        requestResetAndGetToken(user.email());

        ResponseEntity<ApiError> response =
                restTemplate.postForEntity(
                        "/api/auth/reset-password",
                        new ResetPasswordRequest(firstToken, "novaSenha123"),
                        ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void forgotPasswordForUnknownEmailReturnsAcceptedWithoutSendingEmail() {
        String email = "ninguem-" + System.nanoTime() + "@finpro.test";

        ResponseEntity<Void> response =
                restTemplate.postForEntity(
                        "/api/auth/forgot-password", new ForgotPasswordRequest(email), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(mailer.lastLinkByEmail).doesNotContainKey(email);
    }

    @Test
    void resetPasswordWithInvalidTokenReturnsBadRequest() {
        ResponseEntity<ApiError> response =
                restTemplate.postForEntity(
                        "/api/auth/reset-password",
                        new ResetPasswordRequest("token-que-nao-existe", "novaSenha123"),
                        ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void resetPasswordWithShortPasswordReturnsBadRequest() {
        ResponseEntity<ApiError> response =
                restTemplate.postForEntity(
                        "/api/auth/reset-password",
                        new ResetPasswordRequest("qualquer", "curta"),
                        ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
