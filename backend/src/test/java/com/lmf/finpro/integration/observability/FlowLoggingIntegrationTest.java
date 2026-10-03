package com.lmf.finpro.integration.observability;

import static org.assertj.core.api.Assertions.assertThat;

import com.lmf.finpro.domain.model.AccountType;
import com.lmf.finpro.infrastructure.web.dto.account.AccountRequest;
import com.lmf.finpro.infrastructure.web.dto.account.AccountResponse;
import com.lmf.finpro.integration.support.AbstractIntegrationTest;
import com.lmf.finpro.integration.support.TestDataFactory;
import com.lmf.finpro.integration.support.TestUser;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
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
                .contains("reason=unknownEmail")
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

    @Test
    void successfulFlowLogsTheIdsItTouchedAndTheListSize(CapturedOutput output) {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);

        ResponseEntity<AccountResponse> created =
                restTemplate.exchange(
                        "/api/accounts",
                        HttpMethod.POST,
                        new HttpEntity<>(
                                new AccountRequest(
                                        "Conta do log", AccountType.CHECKING, BigDecimal.TEN),
                                user.authHeaders()),
                        AccountResponse.class);
        restTemplate.exchange(
                "/api/accounts",
                HttpMethod.GET,
                new HttpEntity<>(user.authHeaders()),
                String.class);

        assertThat(output.getAll())
                .contains("Fluxo Account.create concluído")
                .contains("resultId=" + created.getBody().id())
                .contains("Fluxo Account.list concluído")
                .contains("resultCount=1");
    }
}
