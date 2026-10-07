package com.lmf.finpro.domain.model;

import java.time.LocalDateTime;

/**
 * Convite para entrar em um grupo compartilhado. Guarda só o hash do token; o token em claro vai
 * apenas no link enviado por e-mail. Uso único e com validade.
 */
public record HouseholdInvite(
        Long id,
        Long householdId,
        String email,
        String tokenHash,
        HouseholdRole role,
        Long createdBy,
        LocalDateTime expiresAt,
        LocalDateTime acceptedAt,
        LocalDateTime createdAt) {

    public static HouseholdInvite issue(
            Long householdId,
            String email,
            String tokenHash,
            Long createdBy,
            LocalDateTime now,
            long ttlDays) {
        return new HouseholdInvite(
                null,
                householdId,
                email,
                tokenHash,
                HouseholdRole.MEMBER,
                createdBy,
                now.plusDays(ttlDays),
                null,
                now);
    }

    public boolean isUsable(LocalDateTime now) {
        return acceptedAt == null && now.isBefore(expiresAt);
    }

    public HouseholdInvite markAccepted(LocalDateTime now) {
        return new HouseholdInvite(
                id, householdId, email, tokenHash, role, createdBy, expiresAt, now, createdAt);
    }
}
