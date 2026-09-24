package com.lmf.finpro.domain.model;

import java.time.LocalDateTime;

/**
 * Token de redefinição de senha. Guarda apenas o hash do token — o valor original só existe no link
 * enviado por e-mail ao usuário.
 */
public record PasswordResetToken(
        Long id,
        Long userId,
        String tokenHash,
        LocalDateTime expiresAt,
        LocalDateTime usedAt,
        LocalDateTime createdAt) {

    public static PasswordResetToken issue(
            Long userId, String tokenHash, LocalDateTime now, long ttlMinutes) {
        return new PasswordResetToken(
                null, userId, tokenHash, now.plusMinutes(ttlMinutes), null, now);
    }

    public boolean isUsable(LocalDateTime now) {
        return usedAt == null && now.isBefore(expiresAt);
    }

    public PasswordResetToken markUsed(LocalDateTime now) {
        return new PasswordResetToken(id, userId, tokenHash, expiresAt, now, createdAt);
    }
}
