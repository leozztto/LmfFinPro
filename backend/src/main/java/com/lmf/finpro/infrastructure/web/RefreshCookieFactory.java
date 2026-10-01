package com.lmf.finpro.infrastructure.web;

import com.lmf.finpro.infrastructure.config.RefreshTokenProperties;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Cookie do refresh token: httpOnly (JavaScript não lê, então XSS não consegue roubá-lo) e restrito
 * ao caminho /api/auth, de modo que só /refresh e /logout o recebem.
 */
@Component
@RequiredArgsConstructor
public class RefreshCookieFactory {

    private final RefreshTokenProperties properties;

    public ResponseCookie create(String rawToken) {
        return base(rawToken).maxAge(Duration.ofDays(properties.ttlDays())).build();
    }

    public ResponseCookie clear() {
        return base("").maxAge(Duration.ZERO).build();
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(RefreshTokenProperties.COOKIE_NAME, value)
                .httpOnly(true)
                .secure(properties.cookieSecure())
                .sameSite(properties.cookieSameSite())
                .path(RefreshTokenProperties.COOKIE_PATH);
    }
}
