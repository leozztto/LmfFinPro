package com.lmf.finpro.infrastructure.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Limites de tentativas dos endpoints públicos de autenticação (proteção contra força bruta e spam
 * de e-mail) — ver {@link com.lmf.finpro.infrastructure.security.AuthRateLimitFilter}.
 *
 * @param enabled desliga todo o rate limit (não usar em produção)
 * @param trustProxyHeader true quando a API só é alcançável através do proxy reverso (nginx), que
 *     define X-Real-IP com o IP real do cliente; false usa o IP da conexão. Com true e a porta da
 *     API exposta diretamente, o cliente consegue forjar o header e escapar do limite.
 * @param login limite por IP no login
 * @param loginPerEmail limite por e-mail no login (ataque distribuído contra uma única conta)
 * @param register limite por IP no cadastro
 * @param forgotPassword limite por IP no "esqueci minha senha"
 * @param forgotPasswordPerEmail limite por e-mail no "esqueci minha senha" (evita inundar a caixa
 *     de uma vítima com e-mails)
 */
@ConfigurationProperties(prefix = "finpro.rate-limit")
public record RateLimitProperties(
        boolean enabled,
        boolean trustProxyHeader,
        Rule login,
        Rule loginPerEmail,
        Rule register,
        Rule forgotPassword,
        Rule forgotPasswordPerEmail) {

    /** No máximo {@code maxRequests} requisições a cada {@code window}. */
    public record Rule(int maxRequests, Duration window) {}
}
