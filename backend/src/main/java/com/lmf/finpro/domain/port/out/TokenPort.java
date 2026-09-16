package com.lmf.finpro.domain.port.out;

public interface TokenPort {
    String generate(Long userId, String email);

    /** Lança InvalidTokenException (RuntimeException) se o token for inválido ou expirado. */
    TokenClaims parse(String token);
}
