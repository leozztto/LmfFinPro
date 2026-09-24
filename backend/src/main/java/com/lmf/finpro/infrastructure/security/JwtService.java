package com.lmf.finpro.infrastructure.security;

import com.lmf.finpro.domain.exception.InvalidTokenException;
import com.lmf.finpro.domain.port.out.TokenClaims;
import com.lmf.finpro.domain.port.out.TokenPort;
import com.lmf.finpro.infrastructure.config.JwtProperties;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtService implements TokenPort {

    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_SESSION_VERSION = "sv";

    private final JwtProperties jwtProperties;

    @Override
    public String generate(Long userId, String email, int sessionVersion) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(CLAIM_EMAIL, email)
                .claim(CLAIM_SESSION_VERSION, sessionVersion)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(jwtProperties.expirationMs())))
                .signWith(signingKey())
                .compact();
    }

    @Override
    public TokenClaims parse(String token) {
        try {
            var claims =
                    Jwts.parser()
                            .verifyWith(signingKey())
                            .build()
                            .parseSignedClaims(token)
                            .getPayload();
            // Tokens anteriores à versão de sessão não têm o claim: valem como versão 0.
            Integer sessionVersion = claims.get(CLAIM_SESSION_VERSION, Integer.class);
            return new TokenClaims(
                    Long.valueOf(claims.getSubject()),
                    claims.get(CLAIM_EMAIL, String.class),
                    sessionVersion == null ? 0 : sessionVersion);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new InvalidTokenException("Token JWT inválido ou expirado");
        }
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }
}
