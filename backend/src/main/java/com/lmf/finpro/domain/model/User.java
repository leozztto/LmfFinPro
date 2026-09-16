package com.lmf.finpro.domain.model;

import java.time.LocalDateTime;

/**
 * Modelo de domínio puro — sem anotações de persistência. A senha já chega
 * como hash: o hashing é responsabilidade do caso de uso, via PasswordHasherPort.
 */
public record User(
    Long id,
    String name,
    String email,
    String passwordHash,
    String taxRegime,
    LocalDateTime createdAt
) {

    public static User register(String name, String email, String passwordHash, String taxRegime) {
        return new User(null, name, email, passwordHash, taxRegime, LocalDateTime.now());
    }
}
