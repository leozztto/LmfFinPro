package com.lmf.finpro.domain.exception;

/** O usuário não pode fazer isso no grupo (não é membro ou não é o dono). */
public class HouseholdPermissionException extends RuntimeException {
    public HouseholdPermissionException(String message) {
        super(message);
    }
}
