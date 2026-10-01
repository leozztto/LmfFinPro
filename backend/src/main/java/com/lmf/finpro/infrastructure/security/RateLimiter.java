package com.lmf.finpro.infrastructure.security;

import com.lmf.finpro.infrastructure.config.RateLimitProperties.Rule;
import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

/**
 * Limitador de janela fixa em memória. Suficiente para uma instância única da API; com várias
 * réplicas cada uma conta separado (o limite efetivo vira N vezes maior) e seria preciso um
 * armazenamento compartilhado (ex.: Redis).
 */
@Component
public class RateLimiter {

    private static final int CLEANUP_EVERY_N_CALLS = 500;

    private record Window(long startMillis, long durationMillis, AtomicInteger count) {}

    private final Clock clock;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final AtomicInteger calls = new AtomicInteger();

    public RateLimiter(Clock clock) {
        this.clock = clock;
    }

    /**
     * Registra uma tentativa para a chave.
     *
     * @return 0 se a tentativa está dentro do limite; senão, os segundos até a janela reabrir
     */
    public long tryAcquire(String key, Rule rule) {
        long now = clock.millis();
        if (calls.incrementAndGet() % CLEANUP_EVERY_N_CALLS == 0) {
            windows.values().removeIf(w -> now - w.startMillis() >= w.durationMillis());
        }

        long durationMillis = rule.window().toMillis();
        Window window =
                windows.compute(
                        key,
                        (k, current) ->
                                current == null || now - current.startMillis() >= durationMillis
                                        ? new Window(now, durationMillis, new AtomicInteger())
                                        : current);

        if (window.count().incrementAndGet() <= rule.maxRequests()) {
            return 0;
        }
        long remainingMillis = window.startMillis() + durationMillis - now;
        return Math.max(1, (remainingMillis + 999) / 1000);
    }
}
