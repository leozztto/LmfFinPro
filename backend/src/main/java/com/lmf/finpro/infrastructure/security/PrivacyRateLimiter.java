package com.lmf.finpro.infrastructure.security;

import com.lmf.finpro.infrastructure.config.RateLimitProperties;
import com.lmf.finpro.infrastructure.config.RateLimitProperties.Rule;
import com.lmf.finpro.infrastructure.web.exception.TooManyRequestsException;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Limite por usuário nas operações de privacidade: a exportação é pesada e a exclusão pede a senha,
 * então uma sessão roubada não pode ficar tentando senhas ali. Respeita o mesmo interruptor geral
 * ({@code finpro.rate-limit.enabled}) dos demais limites.
 */
@Component
@RequiredArgsConstructor
public class PrivacyRateLimiter {

    private static final Rule EXPORT = new Rule(5, Duration.ofMinutes(10));
    private static final Rule DELETE_ACCOUNT = new Rule(5, Duration.ofMinutes(15));

    private final RateLimitProperties properties;
    private final RateLimiter rateLimiter;

    public void checkExport(Long userId) {
        check("export", userId, EXPORT);
    }

    public void checkDeleteAccount(Long userId) {
        check("delete-account", userId, DELETE_ACCOUNT);
    }

    private void check(String scope, Long userId, Rule rule) {
        if (!properties.enabled()) {
            return;
        }
        long retryAfterSeconds = rateLimiter.tryAcquire("user:" + scope + ":" + userId, rule);
        if (retryAfterSeconds > 0) {
            throw new TooManyRequestsException(
                    AuthEmailRateLimiter.message(retryAfterSeconds), retryAfterSeconds);
        }
    }
}
