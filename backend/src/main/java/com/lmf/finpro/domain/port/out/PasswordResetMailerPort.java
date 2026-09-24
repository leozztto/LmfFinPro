package com.lmf.finpro.domain.port.out;

public interface PasswordResetMailerPort {
    void sendResetLink(String toEmail, String userName, String resetLink, long ttlMinutes);
}
