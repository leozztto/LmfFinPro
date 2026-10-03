package com.lmf.finpro.integration.observability;

import static org.assertj.core.api.Assertions.assertThat;

import com.lmf.finpro.infrastructure.scheduling.RefreshTokenCleanupScheduler;
import com.lmf.finpro.integration.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

// @SpringBootTest desliga os exportadores de métricas (inclusive o Prometheus) por padrão.
@AutoConfigureObservability
class ObservabilityIntegrationTest extends AbstractIntegrationTest {

    @LocalManagementPort private int managementPort;

    @Value("${local.server.port}")
    private int serverPort;

    @Autowired private RefreshTokenCleanupScheduler refreshTokenCleanupScheduler;

    @Test
    void prometheusEndpointOnManagementPortExposesSchedulerMetrics() {
        refreshTokenCleanupScheduler.purgeExpired();

        ResponseEntity<String> response =
                restTemplate.getForEntity(
                        "http://localhost:" + managementPort + "/actuator/prometheus",
                        String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .contains("finpro_scheduler_runs_total")
                .contains("scheduler=\"refreshTokenCleanup\"")
                .contains("finpro_scheduler_last_success_timestamp_seconds");
    }

    @Test
    void actuatorIsNotServedOnTheApiPort() {
        ResponseEntity<String> response =
                restTemplate.getForEntity(
                        "http://localhost:" + serverPort + "/actuator/prometheus", String.class);

        assertThat(response.getStatusCode()).isNotEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).doesNotContain("finpro_scheduler");
    }

    @Test
    void unknownPathIsA404NotA500() {
        ResponseEntity<String> response =
                restTemplate.getForEntity(
                        "http://localhost:" + serverPort + "/api/auth/nao-existe", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void malformedJsonIsA400NotA500() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response =
                restTemplate.postForEntity(
                        "http://localhost:" + serverPort + "/api/auth/login",
                        new HttpEntity<>("{nao e json", headers),
                        String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void unsafeIncomingRequestIdIsReplaced() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Request-Id", "abc 123 fake-log-line");

        ResponseEntity<String> response =
                restTemplate.exchange(
                        "http://localhost:" + serverPort + "/api/auth/nao-existe",
                        HttpMethod.GET,
                        new HttpEntity<>(headers),
                        String.class);

        assertThat(response.getHeaders().getFirst("X-Request-Id")).matches("[0-9a-f-]{36}");
    }

    @Test
    void responsesCarryARequestId() {
        ResponseEntity<String> response =
                restTemplate.getForEntity(
                        "http://localhost:" + serverPort + "/api/auth/nope", String.class);

        assertThat(response.getHeaders().getFirst("X-Request-Id")).isNotBlank();
    }
}
