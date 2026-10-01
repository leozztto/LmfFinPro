package com.lmf.finpro.integration.observability;

import static org.assertj.core.api.Assertions.assertThat;

import com.lmf.finpro.integration.support.AbstractIntegrationTest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** O log precisa dizer qual fluxo rodou e por que um erro tratado foi devolvido ao usuário. */
@ExtendWith(OutputCaptureExtension.class)
class FlowLoggingIntegrationTest extends AbstractIntegrationTest {

    @Test
    void failedLoginLogsFlowRejectionAndAccessWithoutLeakingTheEmail(CapturedOutput output) {
        ResponseEntity<String> response =
                restTemplate.postForEntity(
                        "/api/auth/login",
                        Map.of("email", "ninguem@example.com", "password", "senha-errada"),
                        String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(output.getAll())
                .contains("Fluxo Auth.login falhou")
                .contains("error=InvalidCredentialsException")
                .contains("Requisição rejeitada POST /api/auth/login -> 401")
                .contains("Requisição HTTP method=POST path=/api/auth/login status=401")
                .doesNotContain("ninguem@example.com")
                .doesNotContain("senha-errada");
    }

    @Test
    void protectedRouteWithoutTokenLogsAnAccessWarning(CapturedOutput output) {
        restTemplate.getForEntity("/api/accounts", String.class);

        assertThat(output.getAll())
                .contains("Requisição HTTP method=GET path=/api/accounts status=401");
    }
}
