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
    DocumentType documentType,
    String documentNumber,
    String phone,
    TaxRegime taxRegime,
    Address address,
    LocalDateTime createdAt
) {

    public static User register(
        String name,
        String email,
        String passwordHash,
        DocumentType documentType,
        String documentNumber,
        String phone,
        TaxRegime taxRegime,
        Address address
    ) {
        return new User(null, name, email, passwordHash, documentType, documentNumber, phone, taxRegime, address, LocalDateTime.now());
    }
}
