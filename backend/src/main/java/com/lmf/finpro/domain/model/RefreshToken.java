package com.lmf.finpro.domain.model;

import java.time.LocalDateTime;

/**
 * Refresh token da sessão. Guarda apenas o hash — o valor original só existe no cookie httpOnly do
 * navegador. Tokens da mesma cadeia de rotações compartilham o {@code familyId}.
 */
public record RefreshToken(
        Long id,
        Long userId,
        String tokenHash,
        String familyId,
        int sessionVersion,
        LocalDateTime expiresAt,
        LocalDateTime revokedAt,
        LocalDateTime createdAt) {

    public static RefreshToken issue(
            Long userId,
            String tokenHash,
            String familyId,
            int sessionVersion,
            LocalDateTime now,
            long ttlDays) {
        return new RefreshToken(
                null,
                userId,
                tokenHash,
                familyId,
                sessionVersion,
                now.plusDays(ttlDays),
                null,
                now);
    }

    public boolean isExpired(LocalDateTime now) {
        return !now.isBefore(expiresAt);
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public RefreshToken revoke(LocalDateTime now) {
        return new RefreshToken(
                id, userId, tokenHash, familyId, sessionVersion, expiresAt, now, createdAt);
    }
}
