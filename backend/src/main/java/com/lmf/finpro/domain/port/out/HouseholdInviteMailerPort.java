package com.lmf.finpro.domain.port.out;

public interface HouseholdInviteMailerPort {
    void sendInvite(
            String toEmail,
            String inviterName,
            String householdName,
            String inviteLink,
            long ttlDays);
}
