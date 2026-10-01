package com.lmf.finpro.infrastructure.scheduling;

import static net.logstash.logback.argument.StructuredArguments.kv;

import com.lmf.finpro.infrastructure.logging.SafeErrors;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * Métricas dos {@code @Scheduled}, para o alerta de "job falhou" ou "job parou de rodar": execuções
 * por resultado, duração, falhas de itens isolados (que os schedulers capturam para não derrubar o
 * lote) e o instante do último sucesso.
 */
@Slf4j
@Component
public class SchedulerMetrics {

    private final MeterRegistry registry;
    private final Clock clock;
    private final Map<String, AtomicLong> lastSuccess = new ConcurrentHashMap<>();

    public SchedulerMetrics(MeterRegistry registry, Clock clock) {
        this.registry = registry;
        this.clock = clock;
    }

    /**
     * Executa o job medindo duração e resultado. Uma exceção do job é registrada (sem a mensagem) e
     * contada como falha, mas não propagada.
     */
    public void run(String job, Runnable body) {
        // Séries de falha existem (em 0) desde a primeira execução: um contador que nasce já em 1
        // não gera increase() no Prometheus, e o primeiro erro passaria sem alerta.
        registry.counter("finpro.scheduler.runs", "scheduler", job, "outcome", "failure");
        itemFailures(job);
        Timer.Sample sample = Timer.start(registry);
        String outcome = "failure";
        long start = System.nanoTime();
        // Todas as linhas de log do job (inclusive as dos serviços) saem com o nome do scheduler.
        MDC.put("scheduler", job);
        log.info("Scheduler {} iniciado", job);
        try {
            body.run();
            outcome = "success";
            lastSuccess
                    .computeIfAbsent(job, this::registerLastSuccessGauge)
                    .set(clock.instant().getEpochSecond());
        } catch (RuntimeException ex) {
            // Registrado aqui, sem a mensagem, e não propagado: o handler padrão do Spring
            // imprimiria a exceção inteira (erros de banco/SMTP trazem valores e e-mails). A falha
            // segue visível na métrica (outcome=failure) e neste log.
            log.error("Scheduler {} falhou: {}", job, SafeErrors.describe(ex));
        } finally {
            log.info(
                    "Scheduler {} finalizado {} {}",
                    job,
                    kv("outcome", outcome),
                    kv("durationMs", (System.nanoTime() - start) / 1_000_000));
            MDC.remove("scheduler");
            sample.stop(registry.timer("finpro.scheduler.duration", "scheduler", job));
            registry.counter("finpro.scheduler.runs", "scheduler", job, "outcome", outcome)
                    .increment();
        }
    }

    /** Falha de um item do lote, já registrada em log pelo scheduler. */
    public void itemFailed(String job) {
        itemFailures(job).increment();
    }

    private Counter itemFailures(String job) {
        return Counter.builder("finpro.scheduler.item.failures")
                .tag("scheduler", job)
                .register(registry);
    }

    private AtomicLong registerLastSuccessGauge(String job) {
        AtomicLong value = new AtomicLong();
        Gauge.builder("finpro.scheduler.last.success.timestamp.seconds", value, AtomicLong::get)
                .tag("scheduler", job)
                .register(registry);
        return value;
    }
}
