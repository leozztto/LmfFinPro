package com.lmf.finpro.domain.model;

public record Client(
        Long id,
        Long userId,
        String name,
        String email,
        String phone,
        DocumentType documentType,
        String documentNumber,
        ClientWorkType workType,
        String notes,
        String color,
        boolean active) {

    public static Client create(
            Long userId,
            String name,
            String email,
            String phone,
            DocumentType documentType,
            String documentNumber,
            ClientWorkType workType,
            String notes,
            String color,
            boolean active) {
        return new Client(
                null,
                userId,
                name,
                email,
                phone,
                documentType,
                documentNumber,
                workType,
                notes,
                color,
                active);
    }

    public boolean belongsTo(Long candidateUserId) {
        return userId.equals(candidateUserId);
    }

    public Client withDetails(
            String newName,
            String newEmail,
            String newPhone,
            DocumentType newDocumentType,
            String newDocumentNumber,
            ClientWorkType newWorkType,
            String newNotes,
            String newColor,
            boolean newActive) {
        return new Client(
                id,
                userId,
                newName,
                newEmail,
                newPhone,
                newDocumentType,
                newDocumentNumber,
                newWorkType,
                newNotes,
                newColor,
                newActive);
    }
}
