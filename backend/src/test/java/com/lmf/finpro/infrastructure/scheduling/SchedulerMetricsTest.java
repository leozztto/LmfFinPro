package com.lmf.finpro.infrastructure.scheduling;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class SchedulerMetricsTest {

    private static final Instant NOW = Instant.parse("2026-10-01T12:00:00Z");

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final SchedulerMetrics metrics =
            new SchedulerMetrics(registry, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void successfulRunCountsSuccessAndRecordsLastSuccess() {
        metrics.run("job", () -> {});

        assertThat(runs("success")).isEqualTo(1);
        assertThat(runs("failure")).isZero();
        assertThat(
                        registry.get("finpro.scheduler.duration")
                                .tag("scheduler", "job")
                                .timer()
                                .count())
                .isEqualTo(1);
        assertThat(
                        registry.get("finpro.scheduler.last.success.timestamp.seconds")
                                .tag("scheduler", "job")
                                .gauge()
                                .value())
                .isEqualTo(NOW.getEpochSecond());
    }

    @Test
    void failedRunCountsFailureLogsWithoutMessageAndKeepsNoLastSuccess(CapturedOutput output) {
        metrics.run(
                "job",
                () -> {
                    throw new IllegalStateException("Key (email)=(maria@example.com) duplicado");
                });

        assertThat(runs("failure")).isEqualTo(1);
        assertThat(runs("success")).isZero();
        assertThat(
                        registry.find("finpro.scheduler.last.success.timestamp.seconds")
                                .tag("scheduler", "job")
                                .gauge())
                .isNull();
        assertThat(output.getAll())
                .contains("Scheduler job falhou: IllegalStateException")
                .doesNotContain("maria@example.com");
    }

    @Test
    void itemFailuresAreCountedPerJob() {
        metrics.itemFailed("job");
        metrics.itemFailed("job");

        assertThat(
                        registry.get("finpro.scheduler.item.failures")
                                .tag("scheduler", "job")
                                .counter()
                                .count())
                .isEqualTo(2);
    }

    @Test
    void finishedLineSummarizesItemsProducedAndFailures(CapturedOutput output) {
        metrics.run(
                "job",
                () -> {
                    metrics.itemDone(3);
                    metrics.itemDone(0);
                    metrics.itemFailed("job");
                });

        assertThat(output.getAll())
                .contains("items=3")
                .contains("produced=3")
                .contains("itemFailures=1");
    }

    private double runs(String outcome) {
        var counter =
                registry.find("finpro.scheduler.runs")
                        .tag("scheduler", "job")
                        .tag("outcome", outcome)
                        .counter();
        return counter == null ? 0 : counter.count();
    }
}
