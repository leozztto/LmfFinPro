package com.lmf.finpro.domain.model;

import java.time.LocalDateTime;

/**
 * Modelo de domínio puro — sem anotações de persistência. A senha já chega como hash: o hashing é
 * responsabilidade do caso de uso, via PasswordHasherPort.
 *
 * <p>{@code sessionVersion} vai dentro de cada token de acesso emitido; tokens com versão diferente
 * da atual são recusados. Incrementá-la encerra todas as sessões abertas do usuário.
 *
 * <p>{@code photoKey} e {@code photoContentType} identificam a foto de perfil no armazenamento de
 * arquivos; ambos são {@code null} quando o usuário não tem foto.
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
        LocalDateTime createdAt,
        int sessionVersion,
        String photoKey,
        String photoContentType) {

    /** Usuário sem foto de perfil. */
    public User(
            Long id,
            String name,
            String email,
            String passwordHash,
            DocumentType documentType,
            String documentNumber,
            String phone,
            TaxRegime taxRegime,
            Address address,
            LocalDateTime createdAt,
            int sessionVersion) {
        this(
                id,
                name,
                email,
                passwordHash,
                documentType,
                documentNumber,
                phone,
                taxRegime,
                address,
                createdAt,
                sessionVersion,
                null,
                null);
    }

    public static User register(
            String name,
            String email,
            String passwordHash,
            DocumentType documentType,
            String documentNumber,
            String phone,
            TaxRegime taxRegime,
            Address address) {
        return new User(
                null,
                name,
                email,
                passwordHash,
                documentType,
                documentNumber,
                phone,
                taxRegime,
                address,
                LocalDateTime.now(),
                0);
    }

    /** Dados cadastrais editáveis pelo próprio usuário; senha, sessão e foto não mudam. */
    public User withProfile(
            String newName,
            String newEmail,
            DocumentType newDocumentType,
            String newDocumentNumber,
            String newPhone,
            TaxRegime newTaxRegime,
            Address newAddress) {
        return new User(
                id,
                newName,
                newEmail,
                passwordHash,
                newDocumentType,
                newDocumentNumber,
                newPhone,
                newTaxRegime,
                newAddress,
                createdAt,
                sessionVersion,
                photoKey,
                photoContentType);
    }

    /** Troca a senha e encerra as sessões abertas (tokens emitidos antes deixam de valer). */
    public User withPasswordHash(String newPasswordHash) {
        return new User(
                id,
                name,
                email,
                newPasswordHash,
                documentType,
                documentNumber,
                phone,
                taxRegime,
                address,
                createdAt,
                sessionVersion + 1,
                photoKey,
                photoContentType);
    }

    /** Define a foto de perfil; {@code null} nos dois parâmetros remove. */
    public User withPhoto(String newPhotoKey, String newPhotoContentType) {
        return new User(
                id,
                name,
                email,
                passwordHash,
                documentType,
                documentNumber,
                phone,
                taxRegime,
                address,
                createdAt,
                sessionVersion,
                newPhotoKey,
                newPhotoContentType);
    }

    public boolean hasPhoto() {
        return photoKey != null;
    }
}
