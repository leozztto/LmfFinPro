package com.lmf.finpro.infrastructure.security;

import com.lmf.finpro.infrastructure.config.RateLimitProperties;
import com.lmf.finpro.infrastructure.config.RateLimitProperties.Rule;
import com.lmf.finpro.infrastructure.web.exception.TooManyRequestsException;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Limite por e-mail nas rotas de autenticação: o filtro por IP não segura um ataque distribuído
 * (vários IPs contra uma mesma conta) nem o spam de e-mails de redefinição para uma caixa-alvo.
 * Contar todas as tentativas (e não só as falhas) permite que um atacante trave o login de uma
 * vítima por uma janela curta; o custo é aceito em troca de simplicidade e de não revelar se a
 * conta existe.
 */
@Component
@RequiredArgsConstructor
public class AuthEmailRateLimiter {

    private final RateLimitProperties properties;
    private final RateLimiter rateLimiter;

    public void checkLogin(String email) {
        check("login", email, properties.loginPerEmail());
    }

    public void checkForgotPassword(String email) {
        check("forgot", email, properties.forgotPasswordPerEmail());
    }

    private void check(String scope, String email, Rule rule) {
        if (!properties.enabled() || email == null) {
            return;
        }
        String key = "email:" + scope + ":" + email.trim().toLowerCase(Locale.ROOT);
        long retryAfterSeconds = rateLimiter.tryAcquire(key, rule);
        if (retryAfterSeconds > 0) {
            throw new TooManyRequestsException(message(retryAfterSeconds), retryAfterSeconds);
        }
    }

    static String message(long retryAfterSeconds) {
        long minutes = (retryAfterSeconds + 59) / 60;
        return "Muitas tentativas. Tente novamente em "
                + (minutes <= 1 ? "1 minuto" : minutes + " minutos")
                + ".";
    }
}
