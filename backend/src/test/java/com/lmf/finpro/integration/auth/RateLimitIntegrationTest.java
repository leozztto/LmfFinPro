package com.lmf.finpro.integration.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.lmf.finpro.infrastructure.web.dto.auth.ForgotPasswordRequest;
import com.lmf.finpro.infrastructure.web.dto.auth.LoginRequest;
import com.lmf.finpro.infrastructure.web.exception.ApiError;
import com.lmf.finpro.integration.support.AbstractIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "finpro.rate-limit.enabled=true")
class RateLimitIntegrationTest extends AbstractIntegrationTest {

    @DynamicPropertySource
    static void lowRateLimits(DynamicPropertyRegistry registry) {
        registry.add("finpro.rate-limit.login.max-requests", () -> "3");
        registry.add("finpro.rate-limit.login-per-email.max-requests", () -> "2");
        registry.add("finpro.rate-limit.forgot-password.max-requests", () -> "4");
        registry.add("finpro.rate-limit.forgot-password-per-email.max-requests", () -> "2");
        registry.add("finpro.rate-limit.status.max-requests", () -> "3");
    }

    private ResponseEntity<ApiError> login(String email) {
        return restTemplate.postForEntity(
                "/api/auth/login", new LoginRequest(email, "senhaErrada1"), ApiError.class);
    }

    private ResponseEntity<ApiError> forgotPassword(String email) {
        return restTemplate.postForEntity(
                "/api/auth/forgot-password", new ForgotPasswordRequest(email), ApiError.class);
    }

    @Test
    void loginIsBlockedPerEmailAfterTooManyAttempts() {
        String email = "brute-" + UUID.randomUUID() + "@finpro.test";

        assertThat(login(email).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(login(email).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        ResponseEntity<ApiError> blocked = login(email);
        assertThat(blocked.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(blocked.getHeaders().getFirst("Retry-After")).isNotNull();
        assertThat(blocked.getBody().message()).contains("Muitas tentativas");
    }

    @Test
    void statusIsLimitedPerIpButStaysPublic() {
        ResponseEntity<String> last = null;
        for (int i = 0; i < 5; i++) {
            last = restTemplate.getForEntity("/api/status", String.class);
            if (last.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                break;
            }
            assertThat(last.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        assertThat(last.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(last.getHeaders().getFirst("Retry-After")).isNotNull();
    }

    @Test
    void forgotPasswordIsBlockedPerEmailToPreventMailSpam() {
        String email = "spam-" + UUID.randomUUID() + "@finpro.test";

        assertThat(forgotPassword(email).getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(forgotPassword(email).getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(forgotPassword(email).getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void forgotPasswordIsBlockedPerIpAcrossDifferentEmails() {
        // O IP é compartilhado com os outros testes da classe, então não assumimos a cota restante:
        // e-mails sempre novos nunca batem no limite por e-mail, só no por IP (máx. 4).
        ResponseEntity<ApiError> blocked = null;
        for (int i = 0; i < 6; i++) {
            blocked = forgotPassword("ip-" + UUID.randomUUID() + "@finpro.test");
            if (blocked.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                break;
            }
        }

        assertThat(blocked.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(blocked.getHeaders().getFirst("Retry-After")).isNotNull();
    }
}
