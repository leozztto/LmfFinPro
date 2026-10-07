package com.lmf.finpro.domain.exception;

/** Convite inexistente, já usado ou expirado. */
public class InvalidHouseholdInviteException extends RuntimeException {
    public InvalidHouseholdInviteException(String message) {
        super(message);
    }
}
