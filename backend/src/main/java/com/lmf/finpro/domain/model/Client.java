package com.lmf.finpro.domain.model;

public record Client(
        Long id,
        Long householdId,
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
            Long householdId,
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
                householdId,
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

    public boolean belongsTo(Long candidateHouseholdId) {
        return householdId.equals(candidateHouseholdId);
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
                householdId,
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
