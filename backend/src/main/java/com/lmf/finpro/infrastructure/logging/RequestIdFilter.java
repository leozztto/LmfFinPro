package com.lmf.finpro.infrastructure.logging;

import static net.logstash.logback.argument.StructuredArguments.kv;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.slf4j.event.Level;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Põe um {@code requestId} no MDC (e no header de resposta) para correlacionar todas as linhas de
 * log de uma requisição, e registra uma linha de acesso ao final dela. Aproveita o {@code
 * X-Request-Id} do proxy quando é seguro; qualquer outro valor é descartado para ninguém injetar
 * quebras de linha ou lixo no log. O {@code userId} é preenchido depois, pelo filtro JWT.
 *
 * <p>A linha de acesso não inclui query string (pode carregar dados sensíveis) e ignora o actuator,
 * que o Prometheus consulta a cada poucos segundos.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Request-Id";
    public static final String MDC_KEY = "requestId";
    public static final String USER_MDC_KEY = "userId";

    private static final Pattern SAFE = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String incoming = request.getHeader(HEADER);
        String requestId =
                incoming != null && SAFE.matcher(incoming).matches()
                        ? incoming
                        : UUID.randomUUID().toString();
        MDC.put(MDC_KEY, requestId);
        response.setHeader(HEADER, requestId);
        long start = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } finally {
            logAccess(request, response, start);
            MDC.remove(MDC_KEY);
            MDC.remove(USER_MDC_KEY);
        }
    }

    private void logAccess(HttpServletRequest request, HttpServletResponse response, long start) {
        String path = request.getRequestURI();
        if (path.startsWith("/actuator")) {
            return;
        }
        int status = response.getStatus();
        long durationMs = (System.nanoTime() - start) / 1_000_000;
        // 5xx e acesso negado (401/403) chamam atenção; o resto é tráfego normal.
        boolean attention = status >= 500 || status == 401 || status == 403;
        log.atLevel(attention ? Level.WARN : Level.INFO)
                .setMessage("Requisição HTTP {} {} {} {}")
                .addArgument(kv("method", request.getMethod()))
                .addArgument(kv("path", path))
                .addArgument(kv("status", status))
                .addArgument(kv("durationMs", durationMs))
                .log();
    }
}
