package com.lmf.finpro.application.status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.lmf.finpro.application.status.PlatformStatus.State;
import com.lmf.finpro.domain.port.out.PlatformHealthPort;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlatformStatusServiceTest {

    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-10-08T15:00:00Z"), ZoneId.of("America/Sao_Paulo"));

    @Mock private PlatformHealthPort platformHealthPort;

    private PlatformStatusService service() {
        return new PlatformStatusService(platformHealthPort, CLOCK);
    }

    @Test
    void isOperationalWhenTheDatabaseResponds() {
        when(platformHealthPort.isDatabaseAvailable()).thenReturn(true);

        PlatformStatus status = service().check();

        assertThat(status.status()).isEqualTo(State.OPERATIONAL);
        assertThat(status.checkedAt()).isEqualTo(LocalDateTime.of(2026, 10, 8, 12, 0));
    }

    @Test
    void isOutageWhenTheDatabaseIsUnavailableEvenThoughTheApiAnswers() {
        when(platformHealthPort.isDatabaseAvailable()).thenReturn(false);

        assertThat(service().check().status()).isEqualTo(State.OUTAGE);
    }
}
