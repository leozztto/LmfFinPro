package com.lmf.finpro.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lmf.finpro.infrastructure.config.RateLimitProperties;
import com.lmf.finpro.infrastructure.config.RateLimitProperties.Rule;
import com.lmf.finpro.infrastructure.web.exception.ApiError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Limita por IP as tentativas em /login, /register e /forgot-password. O limite por e-mail (que
 * exige ler o corpo da requisição) fica em {@link AuthEmailRateLimiter}, chamado pelo controller.
 */
@Component
@RequiredArgsConstructor
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private final RateLimitProperties properties;
    private final RateLimiter rateLimiter;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !properties.enabled()
                || !HttpMethod.POST.matches(request.getMethod())
                || ruleFor(request.getRequestURI()) == null;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        long retryAfterSeconds =
                rateLimiter.tryAcquire("ip:" + path + ":" + clientIp(request), ruleFor(path));
        if (retryAfterSeconds > 0) {
            writeTooManyRequests(request, response, retryAfterSeconds);
            return;
        }
        chain.doFilter(request, response);
    }

    private Rule ruleFor(String path) {
        Map<String, Rule> rules =
                Map.of(
                        "/api/auth/login", properties.login(),
                        "/api/auth/register", properties.register(),
                        "/api/auth/forgot-password", properties.forgotPassword());
        return rules.get(path);
    }

    private String clientIp(HttpServletRequest request) {
        if (properties.trustProxyHeader()) {
            String realIp = request.getHeader("X-Real-IP");
            if (realIp != null && !realIp.isBlank()) {
                return realIp.trim();
            }
        }
        return request.getRemoteAddr();
    }

    private void writeTooManyRequests(
            HttpServletRequest request, HttpServletResponse response, long retryAfterSeconds)
            throws IOException {
        ApiError body =
                new ApiError(
                        LocalDateTime.now(),
                        HttpStatus.TOO_MANY_REQUESTS.value(),
                        HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
                        AuthEmailRateLimiter.message(retryAfterSeconds),
                        request.getRequestURI());
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
