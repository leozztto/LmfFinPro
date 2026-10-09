package com.lmf.finpro.integration.status;

import static org.assertj.core.api.Assertions.assertThat;

import com.lmf.finpro.integration.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class StatusIntegrationTest extends AbstractIntegrationTest {

    @Test
    void statusIsPublicAndExposesOnlyTheOverallState() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/status", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getCacheControl()).contains("no-store");
        assertThat(response.getBody())
                .contains("\"status\":\"OPERATIONAL\"")
                .contains("checkedAt")
                .doesNotContain("components")
                .doesNotContain("database");
    }
}
